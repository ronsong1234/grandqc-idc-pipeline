GrandQC QuPath Annotation Loader — TCGA_BRCA_0.5MPP
=====================================================
1. Extract zip to local folder
2. Open QuPath, create project, add TIFFs from tiff/ folder
3. Open Automate > Script Editor
4. Paste grandqc_load_annotations.groovy
5. Set GEOJSON_ROOT to your local geojsons path (forward slashes)
6. Open a slide and click Run
7. RESTART QuPath to see saved annotations

Colors: No Artifact=green  Fold=orange  OOF=red
        Air Bubble=cyan  Pen Marking=magenta  Dark Spot=gray

Magnification: 7x (MPP 1.5) — GrandQC paper benchmark
Resolution: 0.5 MPP (downsampled from native 0.25 MPP)
Pass/fail: PASS>=80%  BORDERLINE 50-80%  FAIL<50% no-artifact

See docs/qupath_setup.md for full instructions.
