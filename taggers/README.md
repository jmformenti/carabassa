# Carabassa Tools

A collection of Python utilities to interact with the Carabassa REST API.

## Requirements

- Python 3.12+
- `uv` (for dependency management)

## Setup

1.  Initialize the project and install dependencies:

    ```bash
    uv sync
    ```

## Tools

### Face Detection (`face_tagger.py`)

This script scans items in a specific Carabassa dataset, detects faces, compares them against the already tagged images (tag `tagger.face.reference`), and tags the items in the dataset with the identified person's name.

**Usage:**

```bash
uv run face_tagger.py --dataset "my-dataset"
```

**Arguments:**

-   `--dataset`: **(Required)** The name of the Carabassa dataset to process.
-   `--api-url`: The Carabassa API URL. Defaults to the `CARABASSA_API_URL` environment variable or `http://localhost:8080/api/`.
-   `--threshold`: Similarity threshold for face matching (default: `0.45`).

**Tags Created:**

-   `person`: identified person name, with face bounding box.
-   `tagger.detect_faces`: processing marker to avoid reprocessing in next runs.

### Duplicate Detection (`duplicate_tagger.py`)

This script computes perceptual hashes (`phash`) for images in a dataset, groups similar images, and tags items as duplicates.

**Usage:**

```bash
uv run duplicate_tagger.py --dataset "my-dataset"
```

**Arguments:**

-   `--dataset`: **(Required)** The name of the Carabassa dataset to process.
-   `--api-url`: The Carabassa API URL. Defaults to the `CARABASSA_API_URL` environment variable or `http://localhost:8080/api/`.
-   `--threshold`: Maximum Hamming distance for similarity grouping (default: `15`).
-   `--force`: Recompute hashes for all images, including already processed ones.
-   `--workers`: Number of worker processes for neighbor search (default: `1`).
-   `--neighbor-chunk-size`: Chunk size per worker in neighbor search (default: `500`).

**Tags Created/Updated:**

-   `phash`: perceptual hash of the image.
-   `tagger.detect_duplicates`: processing marker to avoid recomputing in next runs.
-   `duplicated`: `true` when item belongs to a duplicate group.
-   `duplicated.group`: duplicate group identifier.

## Library (`dataset_api_service.py`)

The core Python client for the Carabassa API. You can use this module to build your own tools.

**Example:**

```python
from dataset_api_service import DatasetApiService
from pathlib import Path

# Initialize service
service = DatasetApiService("http://localhost:8080/api/")

# Get dataset ID
dataset_id = service.find_by_name("my-dataset")

# Find all items
items = service.find_items(dataset_id)

# Download an item
file_path = service.get_item(dataset_id, items[0].id)
print(f"Downloaded to: {file_path}")
```

## Configuration

**Environment Variables:**

-   `CARABASSA_API_URL`: Base URL for the Carabassa API (default: `http://localhost:8080/api/`).
-   `CARABASSA_REPO_CACHE`: Directory used to cache downloaded files (default: `~/.cache/carabassa`).
