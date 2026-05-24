# Carabassa MCP Server

MCP (Model Context Protocol) server that exposes the [Carabassa](https://github.com/jmformenti/carabassa) REST API as tools for AI agents.

## Requirements

- [uv](https://docs.astral.sh/uv/) (includes `uvx`)
- A running Carabassa instance

## Run

```bash
# Read-only mode (default): only query/read tools are exposed
uvx carabassa-mcp

# Full mode: enables create/update/delete tools as well
CARABASSA_WRITE_ENABLED=true uvx carabassa-mcp

# Custom backend URL
CARABASSA_BASE_URL=http://my-server:8080 uvx carabassa-mcp
```

> Before publishing to PyPI, run from the local source:
> ```bash
> uvx --from /path/to/carabassa/mcp-server carabassa-mcp
> ```

## Configure in your MCP client

Use the following parameters in your MCP client configuration:

| Parameter | Value |
|-----------|-------|
| command | `uvx` |
| args | `["carabassa-mcp"]` |
| env | `CARABASSA_BASE_URL`, `CARABASSA_WRITE_ENABLED` |

> Before publishing to PyPI, use args `["--from", "/path/to/carabassa/mcp-server", "carabassa-mcp"]`.

Set `CARABASSA_WRITE_ENABLED` to `"true"` to enable create/update/delete tools.

## Available tools

### Datasets
| Tool | Description |
|------|-------------|
| `list_datasets` | List all datasets (paginated) |
| `get_dataset` | Get dataset by ID |
| `get_dataset_by_name` | Get dataset by name |
| `create_dataset` | Create a new dataset *(write mode only)* |
| `update_dataset` | Update dataset name/description *(write mode only)* |
| `delete_dataset` | Delete a dataset and all its items *(write mode only)* |

### Items
| Tool | Description |
|------|-------------|
| `list_items` | List items with optional search filter and pagination |
| `get_item` | Get full item details including tags |
| `item_exists_by_hash` | Check if an item exists by its file hash |
| `add_item` | Upload a local file to a dataset *(write mode only)* |
| `delete_item` | Delete an item *(write mode only)* |
| `reindex_item` | Reindex an item (recalculate hash, type, date, tags) *(write mode only)* |
| `get_item_content_url` | Get download URL for item file |
| `get_item_thumbnail_url` | Get URL for item thumbnail |

### Tags
| Tool | Description |
|------|-------------|
| `add_item_tag` | Add a name/value tag to an item *(write mode only)* |
| `delete_item_tag` | Remove a tag from an item *(write mode only)* |
| `list_items_by_tag` | List items that have a specific tag name |
| `list_tag_values` | List all distinct values for a tag name in a dataset |

### Tag info (metadata)
| Tool | Description |
|------|-------------|
| `list_tag_infos` | List all tag metadata entries |
| `get_tag_info` | Get a single tag info entry |
| `create_tag_info` | Create a tag info entry *(write mode only)* |
| `update_tag_info` | Update a tag info entry *(write mode only)* |
| `delete_tag_info` | Delete a tag info entry *(write mode only)* |

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `CARABASSA_BASE_URL` | `http://localhost:8080` | Base URL of the Carabassa backend |
| `CARABASSA_WRITE_ENABLED` | `false` | Set to `true` to enable create/update/delete tools |

## Publish to PyPI

```bash
uv build
uv publish --token pypi-XXXX...
```
