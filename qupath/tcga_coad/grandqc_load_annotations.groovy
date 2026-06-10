// ── CONFIG ────────────────────────────────────────────────────────────────────
def GEOJSON_ROOT = "REPLACE_WITH_YOUR_PATH"  // e.g. "C:/Users/you/Desktop/tcga_coad_grandqc_qupath/geojsons"

def classColors = [
    "No Artifact"              : ColorTools.makeRGB(0,   200, 0),
    "Fold"                     : ColorTools.makeRGB(255, 140, 0),
    "Dark Spot"                : ColorTools.makeRGB(80,  80,  80),
    "Pen Marking"              : ColorTools.makeRGB(255, 0,   255),
    "Air Bubble"               : ColorTools.makeRGB(0,   180, 255),
    "OOF"                      : ColorTools.makeRGB(255, 0,   0),
    "Darkspot & Foreign Object": ColorTools.makeRGB(80,  80,  80),
]

def imageData = getCurrentImageData()
def imageName = imageData.getServer().getMetadata().getName()
def geojsonFile = new File(GEOJSON_ROOT + "/7x/" + imageName + ".geojson")

print "Image: " + imageName
print "GeoJSON exists: " + geojsonFile.exists()
if (!geojsonFile.exists()) { print "ERROR: not found"; return }

import com.google.gson.JsonParser
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane

def plane = ImagePlane.getDefaultPlane()
def root = JsonParser.parseString(geojsonFile.text).getAsJsonObject()
def features = root.getAsJsonArray("features")

def loaded = 0
def skipped = 0

features.each { feature ->
    try {
        def props = feature.getAsJsonObject("properties")
        def label = props.has("classification") ?
            props.get("classification").getAsString() : "Unknown"

        def geom = feature.getAsJsonObject("geometry")
        def coords = geom.getAsJsonArray("coordinates").get(0).getAsJsonArray()

        def xs = []
        def ys = []
        coords.each { pt ->
            xs.add(pt.get(0).getAsDouble())
            ys.add(pt.get(1).getAsDouble())
        }

        def x = xs.min()
        def y = ys.min()
        def w = xs.max() - x
        def h = ys.max() - y

        if (w < 2 || h < 2) { skipped++; return }

        def roi = ROIs.createRectangleROI(x, y, w, h, plane)
        def color = classColors[label] ?: ColorTools.makeRGB(128,128,128)
        def pathClass = PathClass.fromString(label, color)
        def annotation = PathObjects.createAnnotationObject(roi, pathClass)
        addObject(annotation)
        loaded++
    } catch (Exception e) {
        skipped++
    }
}

fireHierarchyUpdate()

def entry = getProjectEntry()
if (entry != null) {
    entry.saveImageData(imageData)
    getProject().syncChanges()
}
print "Loaded " + loaded + " annotations, skipped " + skipped
