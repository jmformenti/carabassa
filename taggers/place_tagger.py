#!/usr/bin/env python3
"""
Location Tagger for Carabassa
-----------------------------
Reads GPS EXIF data (via existing metadata tags) from items and returns the closest city or town.

Dependencies:
    pip install geopy
"""

import json
import logging
import sys
import time
from pathlib import Path
from typing import List, Tuple, Optional
from tqdm import tqdm
from geopy.geocoders import Nominatim
from geopy.distance import geodesic
from geopy.exc import GeocoderTimedOut, GeocoderServiceError

# Add current directory to path to import dataset_tagger
sys.path.append(str(Path(__file__).parent))
from dataset_tagger import DatasetTagger
from dataset_api_service import Tag

logger = logging.getLogger(__name__)

# Constants
SOURCE_TAG_NAME = "tagger.place.reference"
TAG_NAME = "tagger.place.location"
TAGGER_TAG_NAME = "tagger.place"

# Constants for spatial calculations to avoid magic numbers
METERS_PER_DEGREE = 111000.0
BOUNDING_BOX_SAFETY_MARGIN = 1.5

TAG_INFO_META = {
    TAG_NAME: {
        "description": "Detected place from GPS coordinates.",
        "alias": "place",
        "internal": False,
        "sortable": False,
        "type": "STRING",
    },
    SOURCE_TAG_NAME: {
        "description": "Reference location tag used to build custom prioritized places.",
        "alias": "place.reference",
        "internal": False,
        "sortable": False,
        "type": "STRING",
    },
    TAGGER_TAG_NAME: {
        "description": "Marker tag indicating the item was processed by the location tagger.",
        "alias": None,
        "internal": True,
        "sortable": False,
        "type": "BOOLEAN",
    },
}

# ---------------------------------------------------------------------------
# Urban / Rural Classification
# ---------------------------------------------------------------------------

_URBAN_FIELDS = {"city", "town"}
_SEMI_RURAL_FIELDS = {"village", "municipality", "suburb", "borough", "quarter"}

_URBAN_TYPES = {
    "city", "town", "borough", "suburb", "quarter",
    "neighbourhood", "city_district",
}

def classify_place(raw: dict) -> str:
    """
    Returns 'urban', 'semi-rural' or 'rural' from Nominatim raw response.
    """
    addr = raw.get("address", {})
    addresstype = raw.get("addresstype", "")
    place_type = raw.get("type", "")

    if addresstype in _URBAN_TYPES or place_type in _URBAN_TYPES:
        return "urban"
    if any(c in addr for c in _URBAN_FIELDS):
        return "urban"
    if any(c in addr for c in _SEMI_RURAL_FIELDS):
        return "semi-rural"
    return "rural"

# ---------------------------------------------------------------------------
# Reverse Geocoding
# ---------------------------------------------------------------------------

_geolocator = Nominatim(user_agent="carabassa_location_tagger/1.0")

def _extract_locality(location) -> Optional[str]:
    """Extract the most specific locality name from the response."""
    if not location:
        return None
    addr = location.raw.get("address", {})
    # Priority: village > town > city > municipality > suburb > county > state
    for field in ("village", "town", "city", "municipality", "suburb", "county", "state"):
        if field in addr:
            return addr[field]
    return location.address

def reverse_geocode(lat: float, lon: float,
                    geolocator: Nominatim,
                    max_urban_dist_km: float = 10.0,
                    max_rural_dist_km: float = 30.0,
                    attempts: int = 3,
                    delay: float = 0.0) -> Tuple[Optional[str], str]:
    """
    Returns (locality, type) where type is 'urban', 'semi-rural', or 'rural'.
    Returns (None, type) if the location is too far.
    """
    if delay > 0:
        time.sleep(delay)

    for attempt in range(attempts):
        try:
            location = geolocator.reverse(
                (lat, lon),
                exactly_one=True,
                language="en",
                timeout=10
            )
            if not location:
                return None, "unknown"

            place_type = classify_place(location.raw)
            max_dist = max_urban_dist_km if place_type in ("urban", "semi-rural") else max_rural_dist_km

            dist = geodesic((lat, lon), (location.latitude, location.longitude)).km
            if dist > max_dist:
                return f"(too far: {dist:.1f} km [{place_type}])", place_type

            return _extract_locality(location), place_type

        except GeocoderTimedOut:
            if attempt < attempts - 1:
                time.sleep(2)
        except GeocoderServiceError as e:
            logger.debug(f"Geocoding error: {e}")
            return None, "unknown"

    return "(timeout)", "unknown"

# ---------------------------------------------------------------------------
# Custom Locations
# ---------------------------------------------------------------------------

class SpatialIndex:
    """
    In-memory spatial index for location caching.
    Supports tiered results: 'custom' (from references) and 'auto' (from previous resolutions).
    """
    def __init__(self, radius_m: float = 200.0):
        self.radius_m = radius_m
        self._entries: List[dict] = []

    def add_place(self, lat: float, lon: float, name: str, place_type: str = "auto"):
        self._entries.append({
            "lat": lat,
            "lon": lon,
            "name": name,
            "type": place_type
        })

    def search(self, lat: float, lon: float) -> Optional[Tuple[str, str]]:
        """
        Returns (name, type) of the closest entry within radius.
        """
        import math
        best_entry = None
        best_dist = float("inf")
        
        # Calculate bounding box deltas
        delta_lat = (self.radius_m / METERS_PER_DEGREE) * BOUNDING_BOX_SAFETY_MARGIN
        # Longitude delta depends on latitude
        lat_rad = math.radians(lat)
        delta_lon = delta_lat / max(math.cos(lat_rad), 0.01)

        for entry in self._entries:
            if abs(lat - entry["lat"]) > delta_lat or abs(lon - entry["lon"]) > delta_lon:
                continue

            dist = geodesic((lat, lon), (entry["lat"], entry["lon"])).meters
            if dist <= self.radius_m and dist < best_dist:
                best_dist = dist
                best_entry = entry

        if best_entry:
            return best_entry["name"], best_entry["type"]
        return None

    @property
    def total(self) -> int:
        return len(self._entries)

# ---------------------------------------------------------------------------
# Tagger Implementation
# ---------------------------------------------------------------------------

class PlaceTagger(DatasetTagger):
    def __init__(self):
        super().__init__(description="GPS Location Tagger")
        self.custom_locations = None
        self.cache = None
        self.local_geolocator = None
        self.public_geolocator = Nominatim(user_agent="carabassa_location_tagger/1.0")
        
        # Statistics
        self.stats = {
            "total_processed": 0,
            "items_with_gps": 0,
            "resolved": 0,
            "sources": {
                "custom": 0,
                "cache": 0,
                "LocalNominatim": 0,
                "PublicNominatim": 0
            }
        }

    def add_custom_args(self, parser):
        parser.add_argument("--max-urban-distance", type=float, default=10.0, help="Max distance for urban/semi-rural places in km (default: 10)")
        parser.add_argument("--max-rural-distance", type=float, default=30.0, help="Max distance for rural places in km (default: 30)")
        parser.add_argument("--delay", type=float, default=1.1, help="Delay between Nominatim API calls in seconds (default: 1.1)")
        parser.add_argument("--custom-radius", type=float, default=500.0, help="Radius in meters to match locations in cache (default: 500)")
        parser.add_argument("--force", action="store_true", help="Force reprocessing of all items")
        parser.add_argument("--global-locations", action="store_true", help="Use a shared custom locations DB across all datasets")
        parser.add_argument("--local-nominatim-url", type=str, help="URL of a local Nominatim instance (e.g., http://localhost:8089)")

    def include_tags_in_search(self) -> bool:
        return True

    @property
    def needs_content(self) -> bool:
        return False

    def get_search_query(self) -> str:
        if self.args.force:
             return "type:I"
        return f"type:I missing_tag:{TAGGER_TAG_NAME}"

    def setup(self):
        if not self._ensure_tag_infos(
            (TAG_NAME, SOURCE_TAG_NAME, TAGGER_TAG_NAME),
            TAG_INFO_META,
        ):
            return False

        # Index for spatial caching (combines reference locations and previous results)
        self.spatial_index = SpatialIndex(radius_m=self.args.custom_radius)
        
        if not self.args.force:
            # 1. Load reference locations (manual tags)
            self._load_spatial_data(SOURCE_TAG_NAME, "reference")
            
            # 2. Load existing resolved locations (auto-cache from previous runs)
            self._load_spatial_data(TAG_NAME, "auto-cache")
        else:
            logger.info("Force mode enabled: skipping load of existing tags (clean start).")

        if self.args.local_nominatim_url:
            url = self.args.local_nominatim_url
            domain = url.split("://")[-1]
            scheme = url.split("://")[0] if "://" in url else "http"
            self.local_geolocator = Nominatim(
                domain=domain,
                scheme=scheme,
                user_agent="carabassa_location_tagger_local"
            )
            logger.info(f"Local Nominatim configured at {url}")

        return True

    def _load_spatial_data(self, tag_name, label):
        logger.info(f"Loading {label} data from tag '{tag_name}'...")
        try:
            # If global mode, we might need to iterate over all datasets, but for now 
            # we'll use find_all_item_tags_by_name if it exists or just current dataset
            
            datasets_to_search = []
            if self.args.global_locations:
                datasets_to_search = self.service.find_datasets()
            else:
                datasets_to_search = [self.service.find_dataset(self.dataset_id)]

            count = 0
            for dataset in datasets_to_search:
                item_tags = self.service.find_dataset_item_tags_by_name(dataset.id, tag_name)
                if not item_tags:
                    continue

                for item_id, infos in tqdm(item_tags.items(), desc=f"Indexing {label} ({dataset.name})", leave=False):
                    try:
                        item = self.service.find_item(dataset.id, item_id)
                        lat, lon = self._get_gps_from_item(item)

                        if lat is None or lon is None:
                            continue

                        for tag_info in infos:
                            location_name = str(tag_info.value)
                            self.spatial_index.add_place(lat, lon, location_name, place_type=label)
                            count += 1

                    except Exception as e:
                        logger.debug(f"Error indexing item {item_id}: {e}")
            
            if count > 0:
                logger.info(f"Indexed {count} entries for {label}.")

        except Exception as e:
            logger.warning(f"Could not load {label} data: {e}")

    def process_item(self, item, img) -> List[Tag]:
        tags = []

        if self.args.force and getattr(item, "tags", None):
            for t in item.tags:
                # Tags are dicts, not objects
                tag_name = t.get("name") if isinstance(t, dict) else t.name
                tag_id = t.get("id") if isinstance(t, dict) else t.id
                if tag_name in (TAG_NAME, TAGGER_TAG_NAME) and tag_id is not None:
                    try:
                        self.service.delete_item_tag(self.dataset_id, item.id, tag_id)
                    except Exception as e:
                        tqdm.write(
                            f"Failed to delete existing tag {tag_name} for item {item.id}: {e}",
                            file=sys.stderr,
                        )

        # Get GPS natively from item tags (pre-computed Carabassa metadata)
        lat, lon = self._get_gps_from_item(item)
        self.stats["total_processed"] += 1

        if lat is not None and lon is not None:
            self.stats["items_with_gps"] += 1
            locality = None
            source = None

            # 1. Spatial Auto-Cache (References + Previous results)
            search_result = self.spatial_index.search(lat, lon)
            if search_result:
                locality, source_type = search_result
                source = "cache" if source_type == "auto-cache" else "custom"

            # 2. Local Nominatim API (No delay)
            if not locality and self.local_geolocator:
                locality, place_type = reverse_geocode(
                    lat, lon,
                    self.local_geolocator,
                    self.args.max_urban_distance,
                    self.args.max_rural_distance,
                    delay=0
                )
                
                if locality and not locality.startswith("("):
                    source = "LocalNominatim"
                    # Add to spatial index immediately for subsequent items
                    self.spatial_index.add_place(lat, lon, locality, place_type="auto-cache")
                else:
                    locality = None

            # 3. Public Nominatim API (With delay)
            if not locality:
                locality, place_type = reverse_geocode(
                    lat, lon,
                    self.public_geolocator,
                    self.args.max_urban_distance,
                    self.args.max_rural_distance,
                    delay=self.args.delay
                )
                
                if locality and not locality.startswith("("):
                    source = "PublicNominatim"
                    # Add to spatial index immediately for subsequent items
                    self.spatial_index.add_place(lat, lon, locality, place_type="auto-cache")
                else:
                    locality = None

            if locality:
                if self.is_debug():
                    tqdm.write(f"Found location for item {item.id}: {locality} (via {source})")
                tags.append(Tag(name=TAG_NAME, value=locality))
                self.stats["resolved"] += 1
                if source in self.stats["sources"]:
                    self.stats["sources"][source] += 1

        # Add tag to mark item as processed
        tags.append(Tag(name=TAGGER_TAG_NAME, value=True))
             
        return tags

    def _get_gps_from_item(self, item) -> Tuple[Optional[float], Optional[float]]:
        lat, lon = None, None
        if getattr(item, "tags", None):
            for t in item.tags:
                # Tags are dicts, not objects
                tag_name = t.get("name") if isinstance(t, dict) else t.name
                tag_value = t.get("value") if isinstance(t, dict) else t.value
                if tag_name == "meta.GeoLatitude":
                    try:
                        lat = float(tag_value)
                    except (ValueError, TypeError):
                        pass
                elif tag_name == "meta.GeoLongitude":
                    try:
                        lon = float(tag_value)
                    except (ValueError, TypeError):
                        pass
        return lat, lon

    def post_process(self):
        print("\n" + "="*40)
        print(" PLACE TAGGER STATISTICS")
        print("="*40)
        print(f" Total items processed:    {self.stats['total_processed']}")
        print(f" Items with GPS info:      {self.stats['items_with_gps']}")
        print(f" Locations resolved:       {self.stats['resolved']}")
        
        if self.stats["resolved"] > 0:
            print("-" * 40)
            print(" Source distribution:")
            for source, count in self.stats["sources"].items():
                if count > 0:
                    pct = (count / self.stats["resolved"]) * 100
                    print(f"   - {source:16}: {count:4} ({pct:5.1f}%)")
        print("="*40)

if __name__ == "__main__":
    tool = PlaceTagger()
    tool.run()
    logger.info("Done.")