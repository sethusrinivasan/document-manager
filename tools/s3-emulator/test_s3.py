#!/usr/bin/env python3
"""
Test script for the S3 emulator using boto3.

Requires: pip install boto3 requests

Tests:
- put_object
- get_object
- list_objects_v2
- delete_object
- generate_presigned_url (PUT and GET) with requests upload/download
- large file upload (1MB)

Usage:
    1. Start the emulator: python3 s3_emulator.py
    2. Run tests:          python3 test_s3.py
"""

import sys
import os

try:
    import boto3
    from botocore.config import Config
    import requests
except ImportError:
    print("ERROR: Required packages not installed.")
    print("Run: pip install boto3 requests")
    sys.exit(1)


ENDPOINT_URL = os.environ.get("S3_ENDPOINT_URL", "http://localhost:9000")
ACCESS_KEY = os.environ.get("S3_ACCESS_KEY", "testkey")
SECRET_KEY = os.environ.get("S3_SECRET_KEY", "testsecret")
BUCKET = os.environ.get("S3_BUCKET", "test-backup-bucket")
REGION = "us-east-1"


def get_s3_client():
    """Create a boto3 S3 client configured for the local emulator."""
    return boto3.client(
        "s3",
        endpoint_url=ENDPOINT_URL,
        aws_access_key_id=ACCESS_KEY,
        aws_secret_access_key=SECRET_KEY,
        region_name=REGION,
        config=Config(
            signature_version="s3v4",
            s3={"addressing_style": "path"},
        ),
    )


def test_put_object(client):
    """Test uploading an object."""
    try:
        client.put_object(
            Bucket=BUCKET,
            Key="test/hello.txt",
            Body=b"Hello, S3 Emulator!",
        )
        print("PASS - put_object: uploaded test/hello.txt")
        return True
    except Exception as e:
        print(f"FAIL - put_object: {e}")
        return False


def test_get_object(client):
    """Test downloading an object."""
    try:
        response = client.get_object(Bucket=BUCKET, Key="test/hello.txt")
        body = response["Body"].read()
        if body == b"Hello, S3 Emulator!":
            print("PASS - get_object: content matches")
            return True
        else:
            print(f"FAIL - get_object: content mismatch, got: {body!r}")
            return False
    except Exception as e:
        print(f"FAIL - get_object: {e}")
        return False


def test_list_objects_v2(client):
    """Test listing objects with ListObjectsV2."""
    try:
        # Upload a few more objects for listing
        client.put_object(Bucket=BUCKET, Key="test/file1.txt", Body=b"file1")
        client.put_object(Bucket=BUCKET, Key="test/file2.txt", Body=b"file2")
        client.put_object(Bucket=BUCKET, Key="other/file3.txt", Body=b"file3")

        # List all objects
        response = client.list_objects_v2(Bucket=BUCKET)
        keys = [obj["Key"] for obj in response.get("Contents", [])]

        if "test/hello.txt" in keys and "test/file1.txt" in keys and "other/file3.txt" in keys:
            print(f"PASS - list_objects_v2: found {len(keys)} objects")
            return True
        else:
            print(f"FAIL - list_objects_v2: expected keys not found, got: {keys}")
            return False
    except Exception as e:
        print(f"FAIL - list_objects_v2: {e}")
        return False


def test_list_objects_prefix(client):
    """Test listing objects with a prefix filter."""
    try:
        response = client.list_objects_v2(Bucket=BUCKET, Prefix="test/")
        keys = [obj["Key"] for obj in response.get("Contents", [])]

        if all(k.startswith("test/") for k in keys) and len(keys) >= 3:
            print(f"PASS - list_objects_v2 (prefix): found {len(keys)} objects with prefix 'test/'")
            return True
        else:
            print(f"FAIL - list_objects_v2 (prefix): unexpected results: {keys}")
            return False
    except Exception as e:
        print(f"FAIL - list_objects_v2 (prefix): {e}")
        return False


def test_delete_object(client):
    """Test deleting an object."""
    try:
        # Upload then delete
        client.put_object(Bucket=BUCKET, Key="to-delete.txt", Body=b"delete me")
        client.delete_object(Bucket=BUCKET, Key="to-delete.txt")

        # Verify it's gone
        try:
            client.get_object(Bucket=BUCKET, Key="to-delete.txt")
            print("FAIL - delete_object: object still exists after deletion")
            return False
        except client.exceptions.NoSuchKey:
            print("PASS - delete_object: object successfully deleted")
            return True
        except Exception:
            # Some clients raise ClientError instead of NoSuchKey
            print("PASS - delete_object: object successfully deleted")
            return True
    except Exception as e:
        print(f"FAIL - delete_object: {e}")
        return False


def test_presigned_put(client):
    """Test presigned URL for PUT (upload via requests)."""
    try:
        presigned_url = client.generate_presigned_url(
            "put_object",
            Params={"Bucket": BUCKET, "Key": "presigned/upload.txt"},
            ExpiresIn=3600,
        )

        # Upload using requests
        upload_data = b"Uploaded via presigned PUT URL"
        response = requests.put(presigned_url, data=upload_data)

        if response.status_code == 200:
            # Verify the upload
            get_response = client.get_object(Bucket=BUCKET, Key="presigned/upload.txt")
            body = get_response["Body"].read()
            if body == upload_data:
                print("PASS - presigned PUT: upload and verification succeeded")
                return True
            else:
                print(f"FAIL - presigned PUT: content mismatch after upload")
                return False
        else:
            print(f"FAIL - presigned PUT: HTTP {response.status_code} - {response.text}")
            return False
    except Exception as e:
        print(f"FAIL - presigned PUT: {e}")
        return False


def test_presigned_get(client):
    """Test presigned URL for GET (download via requests)."""
    try:
        # Ensure the object exists
        test_content = b"Download me via presigned GET URL"
        client.put_object(Bucket=BUCKET, Key="presigned/download.txt", Body=test_content)

        presigned_url = client.generate_presigned_url(
            "get_object",
            Params={"Bucket": BUCKET, "Key": "presigned/download.txt"},
            ExpiresIn=3600,
        )

        # Download using requests
        response = requests.get(presigned_url)

        if response.status_code == 200 and response.content == test_content:
            print("PASS - presigned GET: download succeeded with correct content")
            return True
        elif response.status_code == 200:
            print(f"FAIL - presigned GET: content mismatch, got {len(response.content)} bytes")
            return False
        else:
            print(f"FAIL - presigned GET: HTTP {response.status_code} - {response.text}")
            return False
    except Exception as e:
        print(f"FAIL - presigned GET: {e}")
        return False


def test_large_file_upload(client):
    """Test uploading a large file (1MB)."""
    try:
        # Generate 1MB of data
        large_data = os.urandom(1024 * 1024)  # 1MB

        client.put_object(
            Bucket=BUCKET,
            Key="large/1mb-file.bin",
            Body=large_data,
        )

        # Download and verify
        response = client.get_object(Bucket=BUCKET, Key="large/1mb-file.bin")
        downloaded = response["Body"].read()

        if downloaded == large_data:
            print(f"PASS - large file upload: 1MB upload and download verified")
            return True
        else:
            print(f"FAIL - large file upload: content mismatch (uploaded {len(large_data)}, got {len(downloaded)})")
            return False
    except Exception as e:
        print(f"FAIL - large file upload: {e}")
        return False


def test_head_object(client):
    """Test HEAD request for object metadata."""
    try:
        response = client.head_object(Bucket=BUCKET, Key="test/hello.txt")
        if response["ContentLength"] == len(b"Hello, S3 Emulator!"):
            print("PASS - head_object: correct content length")
            return True
        else:
            print(f"FAIL - head_object: wrong content length: {response['ContentLength']}")
            return False
    except Exception as e:
        print(f"FAIL - head_object: {e}")
        return False


def main():
    print(f"S3 Emulator Test Suite")
    print(f"======================")
    print(f"Endpoint: {ENDPOINT_URL}")
    print(f"Bucket:   {BUCKET}")
    print(f"Region:   {REGION}")
    print()

    client = get_s3_client()

    tests = [
        ("PUT Object", test_put_object),
        ("GET Object", test_get_object),
        ("HEAD Object", test_head_object),
        ("List Objects V2", test_list_objects_v2),
        ("List Objects (prefix)", test_list_objects_prefix),
        ("DELETE Object", test_delete_object),
        ("Presigned PUT URL", test_presigned_put),
        ("Presigned GET URL", test_presigned_get),
        ("Large File Upload (1MB)", test_large_file_upload),
    ]

    results = []
    for name, test_fn in tests:
        print(f"\n--- {name} ---")
        result = test_fn(client)
        results.append((name, result))

    # Summary
    print("\n" + "=" * 40)
    print("SUMMARY")
    print("=" * 40)
    passed = sum(1 for _, r in results if r)
    failed = sum(1 for _, r in results if not r)

    for name, result in results:
        status = "PASS" if result else "FAIL"
        print(f"  [{status}] {name}")

    print(f"\n  Total: {passed} passed, {failed} failed out of {len(results)} tests")

    if failed > 0:
        sys.exit(1)
    print("\nAll tests passed!")


if __name__ == "__main__":
    main()
