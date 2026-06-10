# QuPath Setup Guide

This guide walks through loading GrandQC artifact annotations into [QuPath](https://qupath.github.io/) for visual inspection of QC results.

---

## Prerequisites

- QuPath 0.5.x or later (free, open-source)
- The QuPath package zip downloaded from Colab (Cell 10 output), e.g.:
  - `TCGA_COAD_1MPP_grandqc_qupath_package.zip`
  - `TCGA_BRCA_0.5MPP_grandqc_qupath_package.zip`

---

## Step-by-Step Instructions

### 1. Extract the zip

Extract to a local folder. You'll get a structure like:

```
TCGA_COAD_grandqc_qupath/
├── tiff/
│   └── colorectal__tcga_coad__*.tiff
├── geojsons/
│   └── 7x/
│       └── colorectal__tcga_coad__*.geojson
├── grandqc_load_annotations.groovy
└── README_QuPath.txt
```

### 2. Create a QuPath project

1. Open QuPath
2. **File → Project → Create project**
3. Point it to a new empty folder

### 3. Add slides

1. **File → Add images**
2. Navigate to the `tiff/` folder and select all `.tiff` files
3. Accept default settings

### 4. Load the annotation script

1. Open **Automate → Script Editor**
2. Open the `.groovy` file from the extracted zip (or paste its contents)

### 5. Set your GeoJSON path

At the top of the script, update `GEOJSON_ROOT` to point to the `geojsons/` folder you extracted:

```groovy
def GEOJSON_ROOT = "C:/Users/you/Desktop/TCGA_COAD_grandqc_qupath/geojsons"
// Use forward slashes — backslashes cause Groovy parse errors on Windows
```

### 6. Run the script

1. In the QuPath image viewer, open a slide
2. In the Script Editor, click **Run**
3. The console should print: `Loaded N annotations, skipped M`

Repeat for each slide (or use **Run for project** to batch-run).

### 7. Restart QuPath

QuPath saves annotations to the project data, but you must **restart QuPath** to see the saved annotations appear in the annotation panel.

---

## Artifact Color Reference

| Artifact Class         | Color    | Hex       |
|------------------------|----------|-----------|
| No Artifact            | Green    | `#00C800` |
| Fold                   | Orange   | `#FF8C00` |
| OOF (Out of Focus)     | Red      | `#FF0000` |
| Air Bubble             | Cyan     | `#00B4FF` |
| Pen Marking            | Magenta  | `#FF00FF` |
| Dark Spot              | Dark Gray| `#505050` |
| Darkspot & Foreign Object | Dark Gray | `#505050` |

---

## QC Verdict Reference

Verdicts are based on `pct_no_artifact` (% of tissue area free of artifacts):

| Verdict     | Threshold              |
|-------------|------------------------|
| PASS        | `pct_no_artifact ≥ 80%` |
| BORDERLINE  | `50% ≤ pct_no_artifact < 80%` |
| FAIL        | `pct_no_artifact < 50%` |

Check `*_qc_summary.csv` and `*_grandqc_histoqc_comparison.csv` in the extracted zip for per-slide metrics.

---

## Magnification Note

GrandQC runs at **7× (MPP 1.5)**, which is the paper's benchmark resolution (Dice 0.808). GeoJSON coordinates are in the coordinate space of the converted TIFF files:

- **TCGA-COAD:** 1.0 MPP
- **TCGA-BRCA:** 0.5 MPP (downsampled from native 0.25 MPP)

When opening the TIFFs in QuPath, the slide resolution is embedded in the TIFF tags, so coordinates should align automatically.

---

## Troubleshooting

**"GeoJSON exists: false"**
→ `GEOJSON_ROOT` path is wrong. Check for typos and ensure you're using forward slashes.

**No annotations visible after Run**
→ Restart QuPath. Annotations are saved but only appear after restart.

**"Loaded 0 annotations, skipped N"**
→ The GeoJSON feature geometry may be incompatible. Re-run Cell 10 in the notebook to re-clean the GeoJSONs with Shapely.

**Misaligned annotations**
→ Open the TIFF from the `tiff/` folder in the extracted zip, not the original DICOM or a separately converted file. The coordinate system must match the conversion from Cell 6.
