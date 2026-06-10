# Changelog

All notable changes to the GrandQC × IDC pipeline are documented here.

---

## [v4] — Current (TCGA-BRCA + TCGA-COAD)

### Added
- TCGA-BRCA cohort support (0.5 MPP, downsampled from native 0.25 MPP)
- HistoQC integration (Cell 9a/9b) with Python 3.12 compatibility patch
- GrandQC vs HistoQC comparison table (Cell 9c) with dual verdict reporting
- QuPath package zip export for both TCGA-COAD and TCGA-BRCA (Cell 10)
- GeoJSON cleaning with Shapely `buffer(0)` + `make_valid()` for JTS compatibility
- `MIN_TD = 512` patch to fix tissue=100% bug on small IDC biopsy slides
- wsidicom streaming for memory-safe DICOM → TIFF conversion (prevents RAM doubling)
- Two-pass TIFF write strategy to avoid 4 GB canvas crash on large TCGA slides
- `hqc_verdict` = `UNRELIABLE` for slides where HistoQC removed all tissue

### Fixed
- `torch.tensor(image_pre)` → `torch.tensor(image_pre.copy())` for PyTorch 2.x non-writable array error
- `weights_only=False` in `main.py` for PyTorch 2.x checkpoint loading
- HistoQC `BaseImage.py` indentation bug (duplicated if/else block) for Python 3.12
- wsidicom API: `WsiDicom.open()` parameter name updated to match current API

---

## [v3] — TCGA-COAD (1 MPP)

### Added
- Initial TCGA-COAD colorectal cohort support
- GrandQC artifact segmentation at 7× (MPP 1.5)
- Tissue detection + artifact classification for: No Artifact, Fold, OOF, Air Bubble, Pen Marking, Dark Spot
- QC metrics computed as % of **tissue area** (not slide canvas)
- Pass/fail thresholds: PASS ≥80%, BORDERLINE 50–80%, FAIL <50%
- OpenSlide MPP KeyError fallback via tifffile `XResolution` tag (`wsi_slide_info.py`)
- MPP fallback for tissue detector line 82 (`wsi_tis_detect.py`)
- Tissue mask convention fix for IDC TIFFs (tissue=black=0)
- `weights_only=False` for PyTorch 2.x

### Pipeline structure
- Cells 1–8: install → clone → patch → config → download → convert → infer → metrics
- QuPath Groovy script for rectangle ROI annotation loading

---

## [v1–v2] — Development / debugging

- Initial pipeline scaffolding and GrandQC integration
- Resolution of PyTorch `weights_only` deprecation warning
- OpenSlide MPP KeyError diagnosis and initial tifffile patch
- Tissue mask polarity debugging (tissue=100% false positive)
