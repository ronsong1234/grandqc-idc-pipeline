# Example Output Files

This folder contains one example slide's QC outputs for each cohort, for reference and testing.

## Files

### TCGA-COAD (Colorectal, 1.0 MPP)

| File | Description |
|------|-------------|
| `tcga_coad_qc_summary.csv` | GrandQC per-slide artifact metrics |
| `tcga_coad_grandqc_histoqc_comparison.csv` | GrandQC vs HistoQC verdict comparison |
| `colorectal__tcga_coad__639626347774.2.0.tiff.geojson` | GrandQC artifact annotations (FeatureCollection, 598 features) |

### TCGA-BRCA (Breast, 0.5 MPP)

| File | Description |
|------|-------------|
| `tcga_brca_qc_summary.csv` | GrandQC per-slide artifact metrics |
| `tcga_brca_grandqc_histoqc_comparison.csv` | GrandQC vs HistoQC verdict comparison |

## QC Summary Schema

| Column | Type | Description |
|--------|------|-------------|
| `tiff_name` | string | Slide filename |
| `magnification` | string | Inference magnification (always `7x`) |
| `pct_tissue_of_slide` | float | % of slide canvas that is tissue |
| `pct_no_artifact` | float | % of tissue area free of artifacts |
| `pct_fold` | float | % of tissue with tissue folds |
| `pct_dark_spot` | float | % of tissue with dark spots |
| `pct_pen_marking` | float | % of tissue with pen markings |
| `pct_air_bubble` | float | % of tissue with air bubbles |
| `pct_out_of_focus` | float | % of tissue that is out of focus |

## Comparison Schema

Extends the QC Summary schema with:

| Column | Type | Description |
|--------|------|-------------|
| `slide_id` | string | Numeric IDC slide identifier |
| `collection` | string | IDC collection name (`tcga_coad`, `tcga_brca`, etc.) |
| `gqc_verdict` | string | `PASS` / `BORDERLINE` / `FAIL` |
| `hqc_pct_blurry` | float | % of tissue flagged as blurry by HistoQC |
| `hqc_tissue_removed` | bool | True if HistoQC cleanup removed all tissue |
| `hqc_verdict` | string | `PASS` / `FAIL` / `UNRELIABLE` |

## GeoJSON Schema

Each feature in the GeoJSON FeatureCollection:

```json
{
  "type": "Feature",
  "properties": {
    "class_id": 2,
    "classification": "Fold",
    "area": 88.875
  },
  "geometry": {
    "type": "Polygon",
    "coordinates": [...]
  }
}
```

**`classification` values:** `No Artifact`, `Fold`, `OOF`, `Air Bubble`, `PenMarking`, `Dark Spot`, `Darkspot & Foreign Object`, `Edge & Air Bubble`
