"""Carabassa MCP server — exposes the Carabassa REST API as MCP tools."""

import os
from typing import Any

import httpx
from mcp.server.fastmcp import FastMCP

BASE_URL = os.environ.get("CARABASSA_BASE_URL", "http://localhost:8080")
API_BASE = f"{BASE_URL}/api"
WRITE_ENABLED = os.environ.get("CARABASSA_WRITE_ENABLED", "false").lower() in ("1", "true", "yes")

mcp = FastMCP("carabassa")

# ---------------------------------------------------------------------------
# HTTP helpers
# ---------------------------------------------------------------------------

def _client() -> httpx.Client:
    timeout = httpx.Timeout(30.0)
    return httpx.Client(base_url=API_BASE, timeout=timeout)


def _get(path: str, params: dict | None = None) -> Any:
    with _client() as c:
        r = c.get(path, params={k: v for k, v in (params or {}).items() if v is not None})
        r.raise_for_status()
        return r.json()


def _post_json(path: str, body: dict) -> Any:
    with _client() as c:
        r = c.post(path, json=body)
        r.raise_for_status()
        return r.json() if r.content else None


def _put_json(path: str, body: dict) -> str:
    with _client() as c:
        r = c.put(path, json=body)
        r.raise_for_status()
        return "OK"


def _delete(path: str) -> str:
    with _client() as c:
        r = c.delete(path)
        r.raise_for_status()
        return "OK"


def _post_file(path: str, file_path: str) -> Any:
    with open(file_path, "rb") as f:
        filename = os.path.basename(file_path)
        files = {"file": (filename, f)}
        with _client() as c:
            r = c.post(path, files=files)
            r.raise_for_status()
            return r.json()


# ---------------------------------------------------------------------------
# Dataset tools
# ---------------------------------------------------------------------------

@mcp.tool()
def list_datasets(page: int = 0, size: int = 20) -> Any:
    """List all datasets with pagination.

    Args:
        page: Page number (0-indexed, default 0).
        size: Page size (default 20).
    """
    return _get("/dataset", {"page": page, "size": size})


@mcp.tool()
def get_dataset(dataset_id: int) -> Any:
    """Get a dataset by its numeric ID.

    Args:
        dataset_id: Numeric dataset identifier.
    """
    return _get(f"/dataset/{dataset_id}")


@mcp.tool()
def get_dataset_by_name(dataset_name: str) -> Any:
    """Get a dataset by its name.

    Args:
        dataset_name: Dataset name.
    """
    return _get(f"/dataset/name/{dataset_name}")


if WRITE_ENABLED:
    @mcp.tool()
    def create_dataset(name: str, description: str = "") -> Any:
        """Create a new dataset.

        Args:
            name: Dataset name (required, must be unique).
            description: Optional description.
        """
        return _post_json("/dataset", {"name": name, "description": description})

    @mcp.tool()
    def update_dataset(dataset_id: int, name: str, description: str = "") -> str:
        """Update an existing dataset's name or description.

        Args:
            dataset_id: Numeric dataset identifier.
            name: New dataset name.
            description: New description.
        """
        return _put_json(f"/dataset/{dataset_id}", {"name": name, "description": description})

    @mcp.tool()
    def delete_dataset(dataset_id: int) -> str:
        """Delete a dataset and all its items.

        Args:
            dataset_id: Numeric dataset identifier.
        """
        return _delete(f"/dataset/{dataset_id}")


# ---------------------------------------------------------------------------
# Item tools
# ---------------------------------------------------------------------------

@mcp.tool()
def list_items(
    dataset_id: int,
    search: str = "",
    include_tags: bool = False,
    page: int = 0,
    size: int = 20,
) -> Any:
    """List items in a dataset with optional search and pagination.

    Search query syntax (space-separated terms are AND-ed; no OR or grouping):

      tag:value           Equals.            e.g. person:Maria
      tag>value           Greater than.      e.g. year>2020
      tag<value           Less than.
      tag>=value          Greater or equal.
      tag<=value          Less or equal.
      "value with spaces" Quote multi-word values.
      on:<date>           Exact date.        YYYY, YYYY-MM or YYYY-MM-DD
      from:<date>         Range start date.  same formats
      to:<date>           Range end date.    same formats
      type:I              Photos only.
      type:V              Videos only.

    Tips:
      - Call list_tag_infos to discover available tag names (person, year, location...).
      - Call list_tag_values(tag_name=...) to verify exact values for a given tag
        (e.g. exact spelling of a person's name) before composing the query.
      - For either-of conditions, pick the most likely value or call list_items
        twice with separate queries.
      - Use type: only when the user explicitly distinguishes photos vs. videos.

    Args:
        dataset_id: Numeric dataset identifier.
        search: Optional search/filter string using the syntax above.
        include_tags: Include full tag details in each item (default False).
        page: Page number (0-indexed).
        size: Page size (default 20).
    """
    params: dict[str, Any] = {"page": page, "size": size, "includeTags": include_tags}
    if search:
        params["search"] = search
    return _get(f"/dataset/{dataset_id}/item", params)


@mcp.tool()
def get_item(dataset_id: int, item_id: int) -> Any:
    """Get full details of a single item, including all its tags.

    Args:
        dataset_id: Numeric dataset identifier.
        item_id: Numeric item identifier.
    """
    return _get(f"/dataset/{dataset_id}/item/{item_id}")


@mcp.tool()
def item_exists_by_hash(dataset_id: int, file_hash: str) -> str:
    """Check whether an item with the given hash exists in the dataset.

    Returns 'exists' or 'not found'.

    Args:
        dataset_id: Numeric dataset identifier.
        file_hash: File hash string.
    """
    with _client() as c:
        r = c.get(f"/dataset/{dataset_id}/item/exists/{file_hash}")
        if r.status_code == 404:
            return "not found"
        r.raise_for_status()
        return "exists"


if WRITE_ENABLED:
    @mcp.tool()
    def add_item(dataset_id: int, file_path: str) -> Any:
        """Upload a file and add it as a new item to a dataset.

        Args:
            dataset_id: Numeric dataset identifier.
            file_path: Absolute path to the file to upload.
        """
        return _post_file(f"/dataset/{dataset_id}/item", file_path)

    @mcp.tool()
    def delete_item(dataset_id: int, item_id: int) -> str:
        """Delete an item from a dataset.

        Args:
            dataset_id: Numeric dataset identifier.
            item_id: Numeric item identifier.
        """
        return _delete(f"/dataset/{dataset_id}/item/{item_id}")

    @mcp.tool()
    def reindex_item(dataset_id: int, item_id: int) -> str:
        """Reindex an item: recalculates hash, file type, archive date, and tags.

        Args:
            dataset_id: Numeric dataset identifier.
            item_id: Numeric item identifier.
        """
        with _client() as c:
            r = c.put(f"/dataset/{dataset_id}/item/{item_id}/reindex")
            r.raise_for_status()
            return "OK"


@mcp.tool()
def get_item_content_url(dataset_id: int, item_id: int) -> str:
    """Return the URL to download the raw content (file) of an item.

    Args:
        dataset_id: Numeric dataset identifier.
        item_id: Numeric item identifier.
    """
    return f"{API_BASE}/dataset/{dataset_id}/item/{item_id}/content"


@mcp.tool()
def get_item_thumbnail_url(dataset_id: int, item_id: int) -> str:
    """Return the URL to fetch the thumbnail image of an item.

    Args:
        dataset_id: Numeric dataset identifier.
        item_id: Numeric item identifier.
    """
    return f"{API_BASE}/dataset/{dataset_id}/item/{item_id}/thumbnail"


# ---------------------------------------------------------------------------
# Tag tools
# ---------------------------------------------------------------------------

if WRITE_ENABLED:
    @mcp.tool()
    def add_item_tag(
        dataset_id: int,
        item_id: int,
        name: str,
        value: str,
    ) -> Any:
        """Add a tag (name/value pair) to an item.

        Args:
            dataset_id: Numeric dataset identifier.
            item_id: Numeric item identifier.
            name: Tag name (e.g. "person", "year", "location").
            value: Tag value (e.g. "John", "2023", "Barcelona").
        """
        return _post_json(f"/dataset/{dataset_id}/item/{item_id}/tag", {"name": name, "value": value})

    @mcp.tool()
    def delete_item_tag(dataset_id: int, item_id: int, tag_id: int) -> str:
        """Remove a tag from an item.

        Args:
            dataset_id: Numeric dataset identifier.
            item_id: Numeric item identifier.
            tag_id: Numeric tag identifier.
        """
        return _delete(f"/dataset/{dataset_id}/item/{item_id}/tag/{tag_id}")


@mcp.tool()
def list_items_by_tag(
    dataset_id: int,
    tag_name: str,
    page: int = 0,
    size: int = 20,
) -> Any:
    """List items that have a specific tag name, with pagination.

    Args:
        dataset_id: Numeric dataset identifier.
        tag_name: Tag name to filter by.
        page: Page number (0-indexed).
        size: Page size (default 20).
    """
    return _get(f"/dataset/{dataset_id}/item/tag/{tag_name}", {"page": page, "size": size})


@mcp.tool()
def list_tag_values(
    dataset_id: int,
    tag_name: str,
    page: int = 0,
    size: int = 50,
) -> Any:
    """List all distinct values for a given tag name in a dataset.

    Args:
        dataset_id: Numeric dataset identifier.
        tag_name: Tag name to retrieve distinct values for.
        page: Page number (0-indexed).
        size: Page size (default 50).
    """
    return _get(f"/dataset/{dataset_id}/item/tag/{tag_name}/values", {"page": page, "size": size})


# ---------------------------------------------------------------------------
# Tag info tools
# ---------------------------------------------------------------------------

@mcp.tool()
def list_tag_infos(page: int = 0, size: int = 50) -> Any:
    """List all tag metadata entries (alias, description, type, etc.).

    Args:
        page: Page number (0-indexed).
        size: Page size (default 50).
    """
    return _get("/tag-info", {"page": page, "size": size})


@mcp.tool()
def get_tag_info(tag_info_id: int) -> Any:
    """Get a single tag info entry by ID.

    Args:
        tag_info_id: Numeric tag info identifier.
    """
    return _get(f"/tag-info/{tag_info_id}")


if WRITE_ENABLED:
    @mcp.tool()
    def create_tag_info(
        tag_name: str,
        description: str = "",
        alias: str = "",
        internal: bool = False,
        sortable: bool = False,
        show_in_help: bool = True,
        value_type: str = "STRING",
    ) -> Any:
        """Create a new tag info metadata entry.

        Args:
            tag_name: Tag name this metadata applies to.
            description: Human-readable description.
            alias: Display alias for the tag.
            internal: Whether the tag is internal/system use only.
            sortable: Whether items can be sorted by this tag.
            show_in_help: Whether to show this tag in help/docs.
            value_type: Value type: BOOLEAN, INTEGER, LONG, FLOAT, DOUBLE, STRING, DATE, DATE_TIME.
        """
        return _post_json("/tag-info", {
            "tagName": tag_name,
            "description": description,
            "alias": alias,
            "internal": internal,
            "sortable": sortable,
            "showInHelp": show_in_help,
            "type": value_type,
        })

    @mcp.tool()
    def update_tag_info(
        tag_info_id: int,
        tag_name: str,
        description: str = "",
        alias: str = "",
        internal: bool = False,
        sortable: bool = False,
        show_in_help: bool = True,
        value_type: str = "STRING",
    ) -> str:
        """Update an existing tag info entry.

        Args:
            tag_info_id: Numeric tag info identifier.
            tag_name: Tag name.
            description: Human-readable description.
            alias: Display alias.
            internal: Internal/system-only flag.
            sortable: Sortable flag.
            show_in_help: Show in help flag.
            value_type: Value type: BOOLEAN, INTEGER, LONG, FLOAT, DOUBLE, STRING, DATE, DATE_TIME.
        """
        return _put_json(f"/tag-info/{tag_info_id}", {
            "tagName": tag_name,
            "description": description,
            "alias": alias,
            "internal": internal,
            "sortable": sortable,
            "showInHelp": show_in_help,
            "type": value_type,
        })

    @mcp.tool()
    def delete_tag_info(tag_info_id: int) -> str:
        """Delete a tag info entry.

        Args:
            tag_info_id: Numeric tag info identifier.
        """
        return _delete(f"/tag-info/{tag_info_id}")


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> None:
    mcp.run()


if __name__ == "__main__":
    main()
