"""
Provides a client to interact with the Carabassa REST API for managing
datasets and their items.
"""

import hashlib
import os
import tempfile
from contextlib import nullcontext
from dataclasses import dataclass
from pathlib import Path
from typing import Generic, TypeVar

import requests
from tqdm import tqdm

T = TypeVar("T")


# ---------------------------------------------------------------------------
# Exceptions
# ---------------------------------------------------------------------------

class ApiException(Exception):
    """Raised when the API returns an error response."""

    def __init__(self, message: str, status_code: int | None = None):
        self.status_code = status_code
        super().__init__(message)


class ItemAlreadyExists(Exception):
    """Raised when trying to add an item that already exists in the dataset."""


# ---------------------------------------------------------------------------
# DTOs / Data classes
# ---------------------------------------------------------------------------

@dataclass
class ItemToUpload:
    """Describes a local file to be uploaded as a dataset item."""

    filename: str
    content_type: str
    path: Path


@dataclass
class Dataset:
    """Represents a dataset returned by the API."""

    id: int | None = None
    name: str | None = None
    description: str | None = None

    @classmethod
    def from_dict(cls, data: dict) -> "DatasetEntity":
        return cls(
            id=data.get("id"),
            name=data.get("name"),
            description=data.get("description"),
        )


@dataclass
class Item:
    """Represents an item inside a dataset."""

    id: int | None = None
    filename: str | None = None
    type: str | None = None
    hash: str | None = None
    tags: list[dict] | None = None

    @classmethod
    def from_dict(cls, data: dict) -> "ItemRepresentation":
        return cls(
            id=data.get("id"),
            filename=data.get("filename"),
            type=data.get("type"),
            hash=data.get("hash"),
            tags=data.get("tags"),
        )


@dataclass
class BoundingBox:
    """Represents a bounding box for a tag."""

    minX: int
    minY: int
    width: int
    height: int

    def to_dict(self) -> dict:
        return {
            "minX": self.minX,
            "minY": self.minY,
            "width": self.width,
            "height": self.height,
        }


@dataclass
class Tag:
    """Represents a tag to be added to an item."""

    name: str | None = None
    value: object = None
    id: int | None = None
    boundingBox: BoundingBox | None = None

    def to_dict(self) -> dict:
        data = {"name": self.name, "value": self.value}
        if self.boundingBox:
            data["boundingBox"] = self.boundingBox.to_dict()
        return data

    @classmethod
    def from_dict(cls, data: dict) -> "Tag":
        return cls(
            id=data.get("id"),
            name=data.get("name"),
            value=data.get("value"),
            boundingBox=None,
        )


@dataclass
class ItemTagInfo:
    """Represents a tag row returned by dataset-wide tag search."""

    item_id: int | None = None
    tag_id: int | None = None
    tag_name: str | None = None
    tag_value: object = None

    @classmethod
    def from_dict(cls, data: dict) -> "ItemTagInfo":
        return cls(
            item_id=data.get("itemId"),
            tag_id=data.get("tagId"),
            tag_name=data.get("tagName"),
            tag_value=data.get("tagValue"),
        )


@dataclass
class PageMetadata:
    """Metadata for a paginated response."""

    size: int
    totalElements: int
    totalPages: int
    number: int


@dataclass
class PagedResult(Generic[T]):
    """A paginated result containing content and page metadata."""

    content: list[T]
    page: PageMetadata


# ---------------------------------------------------------------------------
# Hash generator (mirrors org.atypical.carabassa.core.util.HashGenerator)
# ---------------------------------------------------------------------------

def generate_hash(file_path: Path) -> str:
    """Generate an MD5 hex digest for the given file."""
    md5 = hashlib.md5()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            md5.update(chunk)
    return md5.hexdigest()


# ---------------------------------------------------------------------------
# Service implementation
# ---------------------------------------------------------------------------

import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class DatasetApiService:
    """
    Python equivalent of ``DatasetApiServiceImpl.java``.

    Usage::

        service = DatasetApiService("http://localhost:8080/")
        dataset_id = service.create("my-dataset", "A test dataset")
        datasets = service.find_all()
    """

    def __init__(self, base_url: str, token: str | None = None, verify_ssl: bool = True) -> None:
        self.base_url = base_url if base_url.endswith("/") else base_url + "/"
        self.session = requests.Session()
        self.session.verify = verify_ssl
        if not verify_ssl:
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        if token:
            self.session.headers.update({"Authorization": f"Bearer {token}"})

    # -- Dataset CRUD -------------------------------------------------------

    def create(self, name: str, description: str | None = None) -> int:
        """Create a new dataset and return its id."""
        if name is None:
            raise ValueError("Name can not be None.")

        payload = {"name": name}
        if description is not None:
            payload["description"] = description

        response = self._post("dataset", json=payload)
        return response["id"]

    # -- Tag info CRUD ------------------------------------------------------

    def create_tag_info(
        self,
        tag_name: str,
        description: str | None = None,
        alias: str | None = None,
        internal: bool | None = None,
        sortable: bool | None = None,
        tag_type: str | None = None,
    ) -> int:
        """Create a new tag info entry and return its id."""
        if tag_name is None:
            raise ValueError("Tag name can not be None.")

        payload: dict[str, object] = {"tagName": tag_name}
        if description is not None:
            payload["description"] = description
        if alias is not None:
            payload["alias"] = alias
        if internal is not None:
            payload["internal"] = internal
        if sortable is not None:
            payload["sortable"] = sortable
        if tag_type is not None:
            payload["type"] = tag_type

        response = self._post("tag-info", json=payload)
        return response["id"]

    def find_all(
        self, page: int | None = None, size: int | None = None
    ) -> list[Dataset] | PagedResult[Dataset]:
        """
        Return datasets.
        If `page` is specified, returns a PagedResult for that page.
        If `page` is None, iterates all pages and returns a list of all datasets.
        """
        params = {}
        if size is not None:
            params["size"] = size

        if page is not None:
            params["page"] = page
            data = self._get("dataset", params=params)
            return self._parse_paged_result(data, "datasetEntityRepresentationList", Dataset.from_dict)
        
        return self._fetch_all_pages("dataset", params, "datasetEntityRepresentationList", Dataset.from_dict, show_progress=False)

    def find_by_name(self, dataset_name: str) -> int:
        """Find a dataset by name and return its id."""
        data = self._get(f"dataset/name/{dataset_name}")
        return data["id"]

    def update(
        self, dataset_id: int, name: str, description: str | None = None
    ) -> None:
        """Update a dataset's name and description."""
        payload = {"name": name}
        if description is not None:
            payload["description"] = description
        self._put(f"dataset/{dataset_id}", json=payload)

    def delete(self, dataset_id: int) -> None:
        """Delete a dataset."""
        self._delete(f"dataset/{dataset_id}")

    # -- Item operations ----------------------------------------------------

    def add_item(self, dataset_id: int, item_to_upload: ItemToUpload) -> int:
        """
        Upload an item to a dataset.

        Raises ``ItemAlreadyExists`` if an item with the same hash already
        exists in the dataset.
        """
        if self._find_item_by_hash(dataset_id, item_to_upload.path):
            raise ItemAlreadyExists("Item already exists.")

        files = {
            "file": (
                item_to_upload.filename,
                open(item_to_upload.path, "rb"),
                item_to_upload.content_type,
            )
        }
        try:
            response = self._post(
                f"dataset/{dataset_id}/item",
                files=files,
            )
        finally:
            files["file"][1].close()

        return response["id"]

    def add_item_tag(
        self, dataset_id: int, item_id: int, tag: Tag
    ) -> int:
        """Add a tag to an item."""
        response = self._post(
            f"dataset/{dataset_id}/item/{item_id}/tag",
            json=tag.to_dict(),
        )
        return response["id"]

    def delete_item_tag(self, dataset_id: int, item_id: int, tag_id: int) -> None:
        """Delete a tag from an item."""
        self._delete(f"dataset/{dataset_id}/item/{item_id}/tag/{tag_id}")

    def find_items(
        self,
        dataset_id: int,
        search_string: str | None = None,
        page: int | None = None,
        size: int | None = None,
        include_tags: bool | None = None,
        sort: str | None = None,
    ) -> list[Item] | PagedResult[Item]:
        """
        Return items in a dataset.
        If `page` is specified, returns a PagedResult for that page.
        If `page` is None, iterates all pages and returns a list of all items.
        """
        params = {}
        if search_string is not None:
            params["search"] = search_string
        if size is not None:
            params["size"] = size
        elif page is None:
            params["size"] = 100  # Default larger batch for fetch-all
        if include_tags is not None:
            params["includeTags"] = "true" if include_tags else "false"
        if sort is not None:
            params["sort"] = sort

        path = f"dataset/{dataset_id}/item"

        if page is not None:
            params["page"] = page
            data = self._get(path, params=params)
            return self._parse_paged_result(data, "itemRepresentationList", Item.from_dict)

        return self._fetch_all_pages(path, params, "itemRepresentationList", Item.from_dict)

    def find_item(self, dataset_id: int, item_id: int) -> Item:
        """Return a single item within a dataset."""
        data = self._get(f"dataset/{dataset_id}/item/{item_id}")
        return Item.from_dict(data)

    def find_dataset_item_tags_by_name(
        self,
        dataset_id: int,
        tag_name: str,
        page: int | None = None,
        size: int | None = None,
        show_progress: bool = True,
    ) -> list[ItemTagInfo] | PagedResult[ItemTagInfo]:
        """
        Return tags with the given name for all items in a dataset.
        If `page` is specified, returns a PagedResult.
        If `page` is None, iterates all pages and returns a list.
        """
        params = {}
        if size is not None:
            params["size"] = size
        elif page is None:
            params["size"] = 100

        path = f"dataset/{dataset_id}/item/tag/{tag_name}"

        if page is not None:
            params["page"] = page
            data = self._get(path, params=params)
            return self._parse_paged_result(data, "itemTagEntityRepresentationList", ItemTagInfo.from_dict)

        return self._fetch_all_pages(
            path,
            params,
            "itemTagEntityRepresentationList",
            ItemTagInfo.from_dict,
            size=size,
            show_progress=show_progress
        )

    def get_item_content(
        self,
        dataset_id: int,
        item_id: int,
        cache: bool = True,
    ) -> Path:
        """
        Get local path to an item's content.

        If ``cache`` is True (default), stores the item in the local cache
        repository (``CARABASSA_REPO_CACHE`` or ``~/.cache/carabassa``) and
        returns its path.

        If ``cache`` is False, downloads to a temporary file and returns its path.
        """
        if not cache:
            return self._download_to_temp(dataset_id, item_id)

        # 1. Get item details to know the hash
        item = self.find_item(dataset_id, item_id)
        if not item.hash:
            # Fallback if no hash is available
            return self._download_to_temp(dataset_id, item_id)

        # 2. Determine cache directory
        cache_dir_env = os.environ.get("CARABASSA_REPO_CACHE")
        if cache_dir_env:
            cache_dir = Path(cache_dir_env)
        else:
            cache_dir = Path.home() / ".cache" / "carabassa"
        cache_dir.mkdir(parents=True, exist_ok=True)

        cached_file = cache_dir / item.hash

        # 3. Check cache or download
        if not cached_file.exists():
            self._download_direct(dataset_id, item_id, cached_file)

        return cached_file

    def _download_to_temp(self, dataset_id: int, item_id: int) -> Path:
        """Helper to download content to a temporary file."""
        fd, temp_path = tempfile.mkstemp()
        os.close(fd)
        path = Path(temp_path)
        self._download_direct(dataset_id, item_id, path)
        return path

    def _download_direct(self, dataset_id: int, item_id: int, destination: Path) -> None:
        """Helper to download content directly to a path."""
        url = self.base_url + f"dataset/{dataset_id}/item/{item_id}/content"
        with self.session.get(url, stream=True) as resp:
            if not resp.ok:
                self._handle_error(resp)
            with open(destination, "wb") as f:
                for chunk in resp.iter_content(chunk_size=8192):
                    f.write(chunk)

    def delete_item(self, dataset_id: int, item_id: int) -> None:
        """Delete a single item from a dataset."""
        self._delete(f"dataset/{dataset_id}/item/{item_id}")

    def reindex(self, dataset_id: int, item_id: int) -> None:
        """Trigger a reindex for a specific item."""
        self._put(f"dataset/{dataset_id}/item/{item_id}/reindex")

    # -- Private helpers ----------------------------------------------------

    def _parse_paged_result(self, data: dict, resource_key: str, mapper: callable) -> PagedResult[T]:
        """Parse a response body into a PagedResult."""
        embedded = data.get("_embedded", {})
        items_data = embedded.get(resource_key, [])
        content = [mapper(i) for i in items_data]
        
        page_info = data.get("page", {})
        metadata = PageMetadata(
            size=page_info.get("size", 0),
            totalElements=page_info.get("totalElements", 0),
            totalPages=page_info.get("totalPages", 0),
            number=page_info.get("number", 0),
        )
        return PagedResult(content=content, page=metadata)

    def _fetch_all_pages(
        self,
        path: str,
        params: dict,
        resource_key: str,
        mapper: callable,
        size: int | None = None,
        show_progress: bool = True,
    ) -> list[T]:
        """Iterate over all pages and return a single list."""
        all_items = []
        current_page = 0
        request_params = dict(params)
        if size is not None:
            request_params["size"] = size

        progress_context = tqdm(desc="Fetching all items", unit="item") if show_progress else nullcontext(None)
        with progress_context as pbar:
            while True:
                request_params["page"] = current_page
                data = self._get(path, params=request_params)

                embedded = data.get("_embedded", {})
                items_data = embedded.get(resource_key, [])
                all_items.extend([mapper(i) for i in items_data])

                page_info = data.get("page", {})
                total_pages = page_info.get("totalPages", 0)
                if pbar is not None:
                    total_elements = page_info.get("totalElements")
                    if isinstance(total_elements, int) and pbar.total is None:
                        pbar.total = total_elements
                    pbar.update(len(items_data))

                if current_page >= total_pages - 1:
                    break
                current_page += 1
            
        return all_items

    def _find_item_by_hash(self, dataset_id: int, file_path: Path) -> bool:
        """Return True if an item with the same hash already exists."""
        file_hash = generate_hash(file_path)
        resp = self.session.get(self.base_url + f"dataset/{dataset_id}/item/exists/{file_hash}")
        if resp.status_code == 404:
            return False
        if not resp.ok:
            self._handle_error(resp)
        return True

    def _handle_error(self, response: requests.Response) -> None:
        """Raise an ``ApiException`` with details from the error response."""
        try:
            body = response.json()
            message = body.get("message", response.text)
        except ValueError:
            message = response.text
        raise ApiException(
            f"{message} (status code: {response.status_code})",
            status_code=response.status_code,
        )

    # -- HTTP wrappers ------------------------------------------------------

    def _get(self, path: str, **kwargs) -> dict | None:
        resp = self.session.get(self.base_url + path, **kwargs)
        if not resp.ok:
            self._handle_error(resp)
        if not resp.text:
            return None
        return resp.json()

    def _post(self, path: str, **kwargs) -> dict | None:
        resp = self.session.post(self.base_url + path, **kwargs)
        if not resp.ok:
            self._handle_error(resp)
        if not resp.text:
            return None
        return resp.json()

    def _put(self, path: str, **kwargs) -> None:
        resp = self.session.put(self.base_url + path, **kwargs)
        if not resp.ok:
            self._handle_error(resp)

    def _delete(self, path: str, **kwargs) -> None:
        resp = self.session.delete(self.base_url + path, **kwargs)
        if not resp.ok:
            self._handle_error(resp)
