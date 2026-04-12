# Carabassa Taggers

A collection of Python taggers that process Carabassa datasets, analyze items, and add tags back into the system.

## Requirements

- Python 3.12+
- `uv` (for dependency management)

## Setup

1.  Initialize the project and install dependencies:

    ```bash
    uv sync
    ```

## Tools

### Common Tagger Parameters

All taggers inherit these parameters from `dataset_tagger.py`:

-   `--dataset`: **(Required)** The name of the Carabassa dataset to process.
-   `--api-url`: The Carabassa API URL. Defaults to the `CARABASSA_API_URL` environment variable or `http://localhost:8080/api/`.
-   `--insecure`: Disable SSL certificate verification (useful for local servers with self-signed certificates).
-   `--debug`: Enable verbose debug logging.

### Face Detection (`face_tagger.py`)

This script scans items in a specific Carabassa dataset, detects faces, compares them against the already tagged images (tag `tagger.face.reference`), and tags the items in the dataset with the identified person's name.

**Usage:**

```bash
uv run face_tagger.py --dataset "my-dataset"
```

**Arguments:**

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

-   `--threshold`: Maximum Hamming distance for similarity grouping (default: `15`).
-   `--force`: Recompute hashes for all images, including already processed ones.
-   `--workers`: Number of worker processes for neighbor search (default: `1`).
-   `--neighbor-chunk-size`: Chunk size per worker in neighbor search (default: `500`).

**Tags Created/Updated:**

-   `phash`: perceptual hash of the image.
-   `tagger.detect_duplicates`: processing marker to avoid recomputing in next runs.
-   `duplicated`: `true` when item belongs to a duplicate group.
-   `duplicated.group`: duplicate group identifier.

### Scene Detection (`scene_tagger.py`)

This script analyzes images in a dataset, detects the scene type (e.g. landscape, indoor, etc.) and tags the items accordingly.

**Usage:**

```bash
uv run scene_tagger.py --dataset "my-dataset"
```

**Arguments:**

-   `--force`: Reprocess all items, including already processed ones.

**Tags Created:**

-   `tagger.scene.label`: detected scene label.
-   `tagger.scene`: processing marker to avoid reprocessing in next runs.

### GPS Location (`place_tagger.py`)

This script reads GPS coordinates from item metadata tags (`meta.GeoLatitude`, `meta.GeoLongitude`) and uses reverse geocoding to tag items with a place name (city, town, or village).

Custom reference locations can be defined directly in the dataset by tagging items with `tagger.place.reference`, which takes priority over the external geocoding service.

**Requirements:**

```bash
uv add geopy
```

**Usage:**

```bash
uv run place_tagger.py --dataset "my-dataset"
```

**Arguments:**

-   `--force`: Reprocess all items, including already tagged ones (replaces existing tags).
-   `--custom-radius`: Radius in meters to match coordinates to custom or cached locations (default: `500`).
-   `--max-urban-distance`: Maximum distance in km to accept an urban/semi-rural result (default: `10.0`).
-   `--max-rural-distance`: Maximum distance in km to accept a rural result (default: `30.0`).
-   `--delay`: Delay in seconds between Nominatim API calls (default: `1.1`).
-   `--global-locations`: Load `tagger.place.reference` items from ALL datasets in the system, not just the current one.
-   `--local-nominatim-url`: URL of a local Nominatim instance (e.g., `http://localhost:8089`) to avoid rate limits and improve speed.

**Tags Created:**

-   `tagger.place.location`: detected place name (city, town, or village).
-   `tagger.place`: processing marker to avoid reprocessing in next runs.

**Custom Reference Locations:**

Tag any item with `tagger.place.reference = "Place Name"` to use it as a custom location anchor. Items tagged this way must also have `meta.GeoLatitude` and `meta.GeoLongitude` metadata. Custom locations take priority over the Nominatim geocoding service.

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
-   `CARABASSA_TOKEN`: JWT authentication token for the Carabassa API. Obtain it by logging in via the API or the web UI.
-   `CARABASSA_REPO_CACHE`: Directory used to cache downloaded files (default: `~/.cache/carabassa`).
