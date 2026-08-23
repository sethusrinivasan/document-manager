# S3 Emulator for Local Testing

A minimal S3-compatible HTTP server (Python 3, no external dependencies) for testing the Document Manager's S3 backup feature without connecting to AWS.

## Quick Start

### 1. Start the Emulator

```bash
cd tools/s3-emulator
python3 s3_emulator.py
```

The server starts on port 9000 with a pre-created `test-backup-bucket`.

### 2. Run Tests

Install test dependencies:

```bash
pip install boto3 requests
```

Run the test suite (in a separate terminal):

```bash
python3 test_s3.py
```

## Default Credentials

| Setting      | Value              |
| ------------ | ------------------ |
| Endpoint URL | http://localhost:9000 |
| Access Key   | `testkey`          |
| Secret Key   | `testsecret`       |
| Region       | `us-east-1`       |
| Bucket       | `test-backup-bucket` |

## Command-Line Options

```
python3 s3_emulator.py [OPTIONS]

  --port PORT          Port to listen on (default: 9000)
  --access-key KEY     AWS access key (default: testkey)
  --secret-key KEY     AWS secret key (default: testsecret)
  --storage-dir DIR    Storage directory (default: ./s3data)
```

## Supported Operations

- **PUT** — Upload objects, create buckets
- **GET** — Download objects, list bucket contents (ListObjectsV2)
- **HEAD** — Check object existence and metadata
- **DELETE** — Delete objects and empty buckets
- **Presigned URLs** — Generate and use presigned PUT/GET URLs

## Configuring the Android App

To point the Document Manager app at the local emulator during development:

1. **Set the endpoint URL** in `S3BackupUploader.kt` or your DI config:

   ```kotlin
   val s3Client = S3Client.builder()
       .endpointOverride(URI("http://10.0.2.2:9000"))  // Android emulator -> host machine
       .region(Region.US_EAST_1)
       .credentialsProvider(
           StaticCredentialsProvider.create(
               AwsBasicCredentials.create("testkey", "testsecret")
           )
       )
       .serviceConfiguration(
           S3Configuration.builder()
               .pathStyleAccessEnabled(true)
               .build()
       )
       .build()
   ```

2. **For physical devices**, replace `10.0.2.2` with your machine's local IP address.

3. **Ensure path-style access** is enabled (the emulator doesn't support virtual-hosted-style bucket URLs).

## Storage

Files are stored on disk at `./s3data/{bucket}/{key}`. Delete the `s3data/` directory to reset all stored data.

## Environment Variables (for test_s3.py)

Override defaults via environment variables:

```bash
S3_ENDPOINT_URL=http://localhost:9000
S3_ACCESS_KEY=testkey
S3_SECRET_KEY=testsecret
S3_BUCKET=test-backup-bucket
```
