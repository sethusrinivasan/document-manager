#!/usr/bin/env python3
"""
Minimal S3-compatible HTTP server for local testing.

Supports PUT, GET, DELETE, HEAD, and ListBucket operations.
Validates AWS Signature V4 auth (simplified: checks access key matches).
Supports presigned URL authentication via query parameters.

Usage:
    python3 s3_emulator.py [--port 9000] [--access-key testkey] [--secret-key testsecret]
"""

import argparse
import hashlib
import hmac
import os
import re
import sys
import json
import urllib.parse
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from http.server import HTTPServer, BaseHTTPRequestHandler
from pathlib import Path


DEFAULT_PORT = 9000
DEFAULT_ACCESS_KEY = "testkey"
DEFAULT_SECRET_KEY = "testsecret"
DEFAULT_BUCKET = "test-backup-bucket"
DEFAULT_REGION = "us-east-1"
STORAGE_DIR = "./s3data"


class S3EmulatorHandler(BaseHTTPRequestHandler):
    """HTTP request handler that emulates core S3 operations."""

    def log_message(self, format, *args):
        """Override to add timestamp prefix."""
        sys.stderr.write(f"[{datetime.now().isoformat()}] {format % args}\n")

    def _get_storage_path(self, bucket, key=""):
        """Get the filesystem path for a bucket/key."""
        base = Path(STORAGE_DIR) / bucket
        if key:
            return base / key
        return base

    def _parse_path(self):
        """Parse the request path into bucket and key."""
        parsed = urllib.parse.urlparse(self.path)
        path_parts = parsed.path.strip("/").split("/", 1)
        bucket = path_parts[0] if path_parts else ""
        key = path_parts[1] if len(path_parts) > 1 else ""
        query_params = urllib.parse.parse_qs(parsed.query)
        return bucket, key, query_params

    def _validate_auth(self, query_params):
        """
        Validate AWS Signature V4 authentication.
        
        Simplified validation: checks that the access key in the Authorization
        header or X-Amz-Credential query param matches the configured key.
        """
        # Check presigned URL auth (query params)
        if "X-Amz-Credential" in query_params:
            credential = query_params["X-Amz-Credential"][0]
            access_key = credential.split("/")[0]
            if access_key == self.server.access_key:
                return True
            self._send_error(403, "InvalidAccessKeyId", "The access key does not match.")
            return False

        # Check Authorization header
        auth_header = self.headers.get("Authorization", "")
        if auth_header.startswith("AWS4-HMAC-SHA256"):
            # Extract credential from: AWS4-HMAC-SHA256 Credential=key/date/region/s3/aws4_request, ...
            match = re.search(r"Credential=([^/]+)/", auth_header)
            if match:
                access_key = match.group(1)
                if access_key == self.server.access_key:
                    return True
                self._send_error(403, "InvalidAccessKeyId", "The access key does not match.")
                return False

        # No auth provided
        self._send_error(403, "AccessDenied", "No valid authentication provided.")
        return False

    def _send_error(self, status, code, message):
        """Send an S3-style XML error response."""
        error_xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<Error>
    <Code>{code}</Code>
    <Message>{message}</Message>
    <RequestId>emulator-request-id</RequestId>
</Error>"""
        self.send_response(status)
        self.send_header("Content-Type", "application/xml")
        body = error_xml.encode("utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_xml(self, status, xml_content):
        """Send an XML response."""
        self.send_response(status)
        self.send_header("Content-Type", "application/xml")
        body = xml_content.encode("utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_PUT(self):
        """Handle PUT requests (upload object or create bucket)."""
        bucket, key, query_params = self._parse_path()

        if not self._validate_auth(query_params):
            return

        if not bucket:
            self._send_error(400, "InvalidBucketName", "Bucket name is required.")
            return

        # Create bucket (PUT /bucket with no key)
        if not key:
            bucket_path = self._get_storage_path(bucket)
            bucket_path.mkdir(parents=True, exist_ok=True)
            self.send_response(200)
            self.send_header("Content-Length", "0")
            self.end_headers()
            return

        # Upload object
        bucket_path = self._get_storage_path(bucket)
        if not bucket_path.exists():
            self._send_error(404, "NoSuchBucket", f"The bucket '{bucket}' does not exist.")
            return

        content_length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_length) if content_length > 0 else b""

        object_path = self._get_storage_path(bucket, key)
        object_path.parent.mkdir(parents=True, exist_ok=True)
        object_path.write_bytes(body)

        # Calculate ETag (MD5 of content)
        etag = hashlib.md5(body).hexdigest()

        self.send_response(200)
        self.send_header("ETag", f'"{etag}"')
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_GET(self):
        """Handle GET requests (download object or list bucket)."""
        bucket, key, query_params = self._parse_path()

        if not self._validate_auth(query_params):
            return

        if not bucket:
            # List all buckets
            self._list_all_buckets()
            return

        bucket_path = self._get_storage_path(bucket)
        if not bucket_path.exists():
            self._send_error(404, "NoSuchBucket", f"The bucket '{bucket}' does not exist.")
            return

        if not key:
            # List objects in bucket
            self._list_bucket(bucket, query_params)
            return

        # Get object
        object_path = self._get_storage_path(bucket, key)
        if not object_path.exists() or object_path.is_dir():
            self._send_error(404, "NoSuchKey", f"The specified key '{key}' does not exist.")
            return

        data = object_path.read_bytes()
        etag = hashlib.md5(data).hexdigest()

        self.send_response(200)
        self.send_header("Content-Type", "application/octet-stream")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("ETag", f'"{etag}"')
        self.send_header("Last-Modified", datetime.now(timezone.utc).strftime("%a, %d %b %Y %H:%M:%S GMT"))
        self.end_headers()
        self.wfile.write(data)

    def do_HEAD(self):
        """Handle HEAD requests (check if object exists)."""
        bucket, key, query_params = self._parse_path()

        if not self._validate_auth(query_params):
            return

        if not bucket:
            self._send_error(400, "InvalidBucketName", "Bucket name is required.")
            return

        if not key:
            # HEAD bucket
            bucket_path = self._get_storage_path(bucket)
            if bucket_path.exists():
                self.send_response(200)
                self.send_header("Content-Length", "0")
                self.end_headers()
            else:
                self.send_response(404)
                self.send_header("Content-Length", "0")
                self.end_headers()
            return

        # HEAD object
        object_path = self._get_storage_path(bucket, key)
        if not object_path.exists() or object_path.is_dir():
            self.send_response(404)
            self.send_header("Content-Length", "0")
            self.end_headers()
            return

        data = object_path.read_bytes()
        etag = hashlib.md5(data).hexdigest()

        self.send_response(200)
        self.send_header("Content-Type", "application/octet-stream")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("ETag", f'"{etag}"')
        self.send_header("Last-Modified", datetime.now(timezone.utc).strftime("%a, %d %b %Y %H:%M:%S GMT"))
        self.end_headers()

    def do_DELETE(self):
        """Handle DELETE requests (delete object)."""
        bucket, key, query_params = self._parse_path()

        if not self._validate_auth(query_params):
            return

        if not bucket:
            self._send_error(400, "InvalidBucketName", "Bucket name is required.")
            return

        if not key:
            # Delete bucket (only if empty)
            bucket_path = self._get_storage_path(bucket)
            if not bucket_path.exists():
                self._send_error(404, "NoSuchBucket", f"The bucket '{bucket}' does not exist.")
                return
            try:
                bucket_path.rmdir()
                self.send_response(204)
                self.send_header("Content-Length", "0")
                self.end_headers()
            except OSError:
                self._send_error(409, "BucketNotEmpty", "The bucket is not empty.")
            return

        # Delete object
        object_path = self._get_storage_path(bucket, key)
        if object_path.exists() and not object_path.is_dir():
            object_path.unlink()

        # S3 returns 204 even if object didn't exist
        self.send_response(204)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def _list_all_buckets(self):
        """List all buckets."""
        storage_path = Path(STORAGE_DIR)
        buckets = []
        if storage_path.exists():
            for item in sorted(storage_path.iterdir()):
                if item.is_dir():
                    buckets.append(item.name)

        bucket_xml_parts = []
        for b in buckets:
            bucket_xml_parts.append(
                f"<Bucket><Name>{b}</Name>"
                f"<CreationDate>{datetime.now(timezone.utc).isoformat()}</CreationDate></Bucket>"
            )

        xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<ListAllMyBucketsResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
    <Owner>
        <ID>emulator</ID>
        <DisplayName>emulator</DisplayName>
    </Owner>
    <Buckets>
        {"".join(bucket_xml_parts)}
    </Buckets>
</ListAllMyBucketsResult>"""
        self._send_xml(200, xml)

    def _list_bucket(self, bucket, query_params):
        """List objects in a bucket (supports list-type=2 for ListObjectsV2)."""
        bucket_path = self._get_storage_path(bucket)
        prefix = query_params.get("prefix", [""])[0]
        delimiter = query_params.get("delimiter", [""])[0]
        max_keys = int(query_params.get("max-keys", ["1000"])[0])
        list_type = query_params.get("list-type", ["1"])[0]

        # Collect all objects
        objects = []
        common_prefixes = set()

        for file_path in sorted(bucket_path.rglob("*")):
            if file_path.is_file():
                key = str(file_path.relative_to(bucket_path))
                if prefix and not key.startswith(prefix):
                    continue

                if delimiter:
                    # Find common prefixes
                    rest = key[len(prefix):]
                    idx = rest.find(delimiter)
                    if idx >= 0:
                        common_prefixes.add(prefix + rest[: idx + len(delimiter)])
                        continue

                stat = file_path.stat()
                etag = hashlib.md5(file_path.read_bytes()).hexdigest()
                objects.append({
                    "key": key,
                    "size": stat.st_size,
                    "etag": etag,
                    "last_modified": datetime.fromtimestamp(
                        stat.st_mtime, tz=timezone.utc
                    ).strftime("%Y-%m-%dT%H:%M:%S.000Z"),
                })

        # Truncate to max_keys
        is_truncated = len(objects) > max_keys
        objects = objects[:max_keys]

        # Build XML response
        contents_xml = ""
        for obj in objects:
            contents_xml += f"""<Contents>
            <Key>{obj['key']}</Key>
            <LastModified>{obj['last_modified']}</LastModified>
            <ETag>"{obj['etag']}"</ETag>
            <Size>{obj['size']}</Size>
            <StorageClass>STANDARD</StorageClass>
        </Contents>
        """

        prefix_xml = ""
        for cp in sorted(common_prefixes):
            prefix_xml += f"<CommonPrefixes><Prefix>{cp}</Prefix></CommonPrefixes>\n"

        if list_type == "2":
            xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
    <Name>{bucket}</Name>
    <Prefix>{prefix}</Prefix>
    <KeyCount>{len(objects)}</KeyCount>
    <MaxKeys>{max_keys}</MaxKeys>
    <IsTruncated>{"true" if is_truncated else "false"}</IsTruncated>
    {contents_xml}
    {prefix_xml}
</ListBucketResult>"""
        else:
            xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
    <Name>{bucket}</Name>
    <Prefix>{prefix}</Prefix>
    <Marker></Marker>
    <MaxKeys>{max_keys}</MaxKeys>
    <IsTruncated>{"true" if is_truncated else "false"}</IsTruncated>
    {contents_xml}
    {prefix_xml}
</ListBucketResult>"""

        self._send_xml(200, xml)


class S3EmulatorServer(HTTPServer):
    """HTTP server with S3 emulator configuration."""

    def __init__(self, port, access_key, secret_key):
        super().__init__(("0.0.0.0", port), S3EmulatorHandler)
        self.access_key = access_key
        self.secret_key = secret_key
        self.port = port


def generate_presigned_url(host, port, bucket, key, access_key, secret_key, method="PUT", expires=3600):
    """Generate a presigned URL for testing."""
    now = datetime.now(timezone.utc)
    datestamp = now.strftime("%Y%m%d")
    amz_date = now.strftime("%Y%m%dT%H%M%SZ")
    region = DEFAULT_REGION
    service = "s3"

    credential_scope = f"{datestamp}/{region}/{service}/aws4_request"
    credential = f"{access_key}/{credential_scope}"

    # Build canonical query string
    params = {
        "X-Amz-Algorithm": "AWS4-HMAC-SHA256",
        "X-Amz-Credential": credential,
        "X-Amz-Date": amz_date,
        "X-Amz-Expires": str(expires),
        "X-Amz-SignedHeaders": "host",
    }

    canonical_querystring = "&".join(
        f"{urllib.parse.quote(k, safe='')}={urllib.parse.quote(v, safe='')}"
        for k, v in sorted(params.items())
    )

    # Build canonical request
    canonical_uri = f"/{bucket}/{key}"
    canonical_headers = f"host:{host}:{port}\n"
    signed_headers = "host"
    payload_hash = "UNSIGNED-PAYLOAD"

    canonical_request = (
        f"{method}\n{canonical_uri}\n{canonical_querystring}\n"
        f"{canonical_headers}\n{signed_headers}\n{payload_hash}"
    )

    # Create string to sign
    string_to_sign = (
        f"AWS4-HMAC-SHA256\n{amz_date}\n{credential_scope}\n"
        f"{hashlib.sha256(canonical_request.encode('utf-8')).hexdigest()}"
    )

    # Calculate signature
    def sign(key, msg):
        return hmac.new(key, msg.encode("utf-8"), hashlib.sha256).digest()

    signing_key = sign(
        sign(
            sign(
                sign(f"AWS4{secret_key}".encode("utf-8"), datestamp),
                region,
            ),
            service,
        ),
        "aws4_request",
    )

    signature = hmac.new(signing_key, string_to_sign.encode("utf-8"), hashlib.sha256).hexdigest()

    presigned_url = (
        f"http://{host}:{port}{canonical_uri}"
        f"?{canonical_querystring}&X-Amz-Signature={signature}"
    )
    return presigned_url


def create_default_bucket():
    """Create the default test bucket on startup."""
    bucket_path = Path(STORAGE_DIR) / DEFAULT_BUCKET
    bucket_path.mkdir(parents=True, exist_ok=True)
    print(f"Created default bucket: {DEFAULT_BUCKET}")


def main():
    global STORAGE_DIR

    parser = argparse.ArgumentParser(description="Minimal S3-compatible HTTP server for local testing")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"Port to listen on (default: {DEFAULT_PORT})")
    parser.add_argument("--access-key", default=DEFAULT_ACCESS_KEY, help=f"AWS access key (default: {DEFAULT_ACCESS_KEY})")
    parser.add_argument("--secret-key", default=DEFAULT_SECRET_KEY, help=f"AWS secret key (default: {DEFAULT_SECRET_KEY})")
    parser.add_argument("--storage-dir", default=STORAGE_DIR, help=f"Storage directory (default: {STORAGE_DIR})")
    args = parser.parse_args()

    STORAGE_DIR = args.storage_dir

    # Ensure storage directory exists
    Path(STORAGE_DIR).mkdir(parents=True, exist_ok=True)

    # Create default bucket
    create_default_bucket()

    # Start server
    server = S3EmulatorServer(args.port, args.access_key, args.secret_key)

    print(f"\nS3 Emulator running on http://localhost:{args.port}")
    print(f"  Access Key: {args.access_key}")
    print(f"  Secret Key: {args.secret_key}")
    print(f"  Storage:    {os.path.abspath(STORAGE_DIR)}")
    print(f"  Region:     {DEFAULT_REGION}")
    print(f"  Bucket:     {DEFAULT_BUCKET}")

    # Generate and print a sample presigned PUT URL
    presigned = generate_presigned_url(
        "localhost", args.port, DEFAULT_BUCKET, "test-upload.txt",
        args.access_key, args.secret_key, method="PUT"
    )
    print(f"\nSample presigned PUT URL:")
    print(f"  {presigned}")
    print(f"\nTest with: curl -X PUT -d 'hello' '{presigned}'")
    print("\nPress Ctrl+C to stop.\n")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down S3 emulator.")
        server.shutdown()


if __name__ == "__main__":
    main()
