package com.airbnb.lottie;

import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class Layer implements Transform {
  private static final String TAG = Layer.class.getSimpleName();
  private final LottieComposition composition;

  private enum LottieLayerType {
    None,
    Solid,
    Unknown,
    Null,
    Shape
  }

  enum MatteType {
    None,
    Add,
    Invert,
    Unknown
  }

  static Layer fromJson(JsonReader reader, LottieComposition composition) throws IOException {
    Layer layer = new Layer(composition);
    layer.frameRate = composition.getFrameRate();

    reader.beginObject();

    while (reader.hasNext()) {
      switch (reader.nextName()) {
        case "nm":
          layer.layerName = reader.nextString();
          break;
        case "ind":
          layer.layerId = reader.nextLong();
          break;
        case "ty":
          int layerType = reader.nextInt();
          if (layerType <= LottieLayerType.Shape.ordinal()) {
            layer.layerType = LottieLayerType.values()[layerType];
          } else {
            layer.layerType = LottieLayerType.Unknown;
          }
          break;
        case "parent":
          layer.parentId = reader.nextLong();
          break;
        case "ip":
          layer.inFrame = reader.nextLong();
          break;
        case "op":
          layer.outFrame = reader.nextLong();
          break;
        case "sw":
          layer.solidWidth = (int) (reader.nextInt() * composition.getScale());
          break;
        case "sh":
          layer.solidHeight = (int) (reader.nextInt() * composition.getScale());
          break;
        case "sc":
          layer.solidColor = Color.parseColor(reader.nextString());
          break;
        case "ks":
          parseTransform(reader, composition, layer);
          break;
        case "tt":
          layer.matteType = MatteType.values()[reader.nextInt()];
          break;
        case "masksProperties":
          parseMasks(reader, composition, layer);
          break;
        case "shapes":


          // TODO (jsonreader)
      }
    }

    reader.endObject();
    try {

      JSONArray shapes = null;
      try {
        shapes = json.getJSONArray("shapes");
      } catch (JSONException e) {
        // Do nothing.
      }
      if (shapes != null) {
        for (int i = 0; i < shapes.length(); i++) {
          Object shape = ShapeGroup.parseShapeItem(shapes.getJSONObject(i), layer.frameRate, composition);
          if (shape != null) {
            layer.shapes.add(shape);
          }
        }
      }
    } catch (JSONException e) {
      throw new IllegalStateException("Unable to parse layer json.", e);
    }

    layer.hasInAnimation = layer.inFrame > composition.getStartFrame();
    layer.hasOutAnimation = layer.outFrame < composition.getEndFrame();
    layer.hasInOutAnimation = layer.hasInAnimation || layer.hasOutAnimation;

    if (layer.hasInOutAnimation) {
      List<Float> keys = new ArrayList<>();
      List<Float> keyTimes = new ArrayList<>();
      long length = composition.getEndFrame() - composition.getStartFrame();

      if (layer.hasInAnimation) {
        keys.add(0f);
        keyTimes.add(0f);
        keys.add(1f);
        float inTime = layer.inFrame / (float) length;
        keyTimes.add(inTime);
      } else {
        keys.add(1f);
        keyTimes.add(0f);
      }

      if (layer.hasOutAnimation) {
        keys.add(0f);
        keyTimes.add(layer.outFrame / (float) length);
        keys.add(0f);
        keyTimes.add(1f);
      } else {
        keys.add(1f);
        keyTimes.add(1f);
      }

      layer.inOutKeyTimes = keyTimes;
      layer.inOutKeyFrames = keys;

    }

    return layer;
  }

  private static void parseTransform(JsonReader reader, LottieComposition composition, Layer layer)
      throws IOException {
    reader.beginObject();

    while (reader.hasNext()) {
      switch (reader.nextName()) {
        case "o":
          layer.opacity = new AnimatableIntegerValue(reader, composition, false, true);
          break;
        case "r":
        case "rz":
          layer.rotation = new AnimatableFloatValue(reader, composition, false);
          break;
        case "p":
          layer.position =
              AnimatablePathValue.createAnimatablePathOrSplitDimensionPath(reader, composition);
          break;
        case "a":
          layer.anchor =
              AnimatablePathValue.createAnimatablePathOrSplitDimensionPath(reader, composition);
          break;
        case "s":
          layer.scale = new AnimatableScaleValue(reader, composition, false);
          break;
        default:
          reader.skipValue();
      }
    }

    reader.endObject();
  }

  private static void parseMasks(JsonReader reader, LottieComposition composition, Layer layer)
      throws IOException{
    reader.beginArray();
    while (reader.hasNext()) {
      layer.masks.add(new Mask(reader, composition));
    }
    reader.endArray();
  }

  private static void parseShaped(JsonReader reader, LottieComposition composition, Layer layer)
      throws IOException {
    reader.beginArray();
    while (reader.hasNext()) {
      // TODO (jsonreader)
    }
    reader.endArray();


    for (int i = 0; i < shapes.length(); i++) {
      Object shape = ShapeGroup.parseShapeItem(shapes.getJSONObject(i), layer.frameRate, composition);
      if (shape != null) {
        layer.shapes.add(shape);
      }
    }
  }

  private final List<Object> shapes = new ArrayList<>();

  private String layerName;
  private long layerId;
  private LottieLayerType layerType;
  private long parentId = -1;
  private long inFrame;
  private long outFrame;
  private int frameRate;

  private final List<Mask> masks = new ArrayList<>();

  private int solidWidth;
  private int solidHeight;
  private int solidColor;

  private AnimatableIntegerValue opacity;
  private AnimatableFloatValue rotation;
  private IAnimatablePathValue position;

  private IAnimatablePathValue anchor;
  private AnimatableScaleValue scale;

  private boolean hasOutAnimation;
  private boolean hasInAnimation;
  private boolean hasInOutAnimation;
  @Nullable private List<Float> inOutKeyFrames;
  @Nullable private List<Float> inOutKeyTimes;

  private MatteType matteType;

  private Layer(LottieComposition composition) {
    this.composition = composition;
  }

  @Override public Rect getBounds() {
    return composition.getBounds();
  }

  @Override public IAnimatablePathValue getAnchor() {
    return anchor;
  }

  LottieComposition getComposition() {
    return composition;
  }

  boolean hasInAnimation() {
    return hasInAnimation;
  }

  boolean hasInOutAnimation() {
    return hasInOutAnimation;
  }

  @Nullable
  List<Float> getInOutKeyFrames() {
    return inOutKeyFrames;
  }

  @Nullable
  List<Float> getInOutKeyTimes() {
    return inOutKeyTimes;
  }

  long getId() {
    return layerId;
  }

  String getName() {
    return layerName;
  }

  List<Mask> getMasks() {
    return masks;
  }

  MatteType getMatteType() {
    return matteType;
  }

  @Override public AnimatableIntegerValue getOpacity() {
    return opacity;
  }

  long getParentId() {
    return parentId;
  }

  @Override public IAnimatablePathValue getPosition() {
    return position;
  }

  @Override public AnimatableFloatValue getRotation() {
    return rotation;
  }

  @Override public AnimatableScaleValue getScale() {
    return scale;
  }

  List<Object> getShapes() {
    return shapes;
  }

  int getSolidColor() {
    return solidColor;
  }

  int getSolidHeight() {
    return solidHeight;
  }

  int getSolidWidth() {
    return solidWidth;
  }

  @Override public String toString() {
    return toString("");
  }

  String toString(String prefix) {
    StringBuilder sb = new StringBuilder();
    sb.append(prefix).append(getName()).append("\n");
    Layer parent = composition.layerModelForId(getParentId());
    if (parent != null) {
      sb.append("\t\tParents: ").append(parent.getName());
      parent = composition.layerModelForId(parent.getParentId());
      while (parent != null) {
        sb.append("->").append(parent.getName());
        parent = composition.layerModelForId(parent.getParentId());
      }
      sb.append(prefix).append("\n");
    }
    if (getPosition().hasAnimation() || getPosition().getInitialPoint().length() != 0) {
      sb.append(prefix).append("\tPosition: ").append(getPosition()).append("\n");
    }
    if (getRotation().hasAnimation() || getRotation().getInitialValue() != 0f) {
      sb.append(prefix).append("\tRotation: ").append(getRotation()).append("\n");
    }
    if (getScale().hasAnimation() || !getScale().getInitialValue().isDefault()) {
      sb.append(prefix).append("\tScale: ").append(getScale()).append("\n");
    }
    if (getAnchor().hasAnimation() || getAnchor().getInitialPoint().length() != 0) {
      sb.append(prefix).append("\tAnchor: ").append(getAnchor()).append("\n");
    }
    if (!getMasks().isEmpty()) {
      sb.append(prefix).append("\tMasks: ").append(getMasks().size()).append("\n");
    }
    if (getSolidWidth() != 0 && getSolidHeight() != 0) {
      sb.append(prefix).append("\tBackground: ").append(String.format(Locale.US, "%dx%d %X\n", getSolidWidth(), getSolidHeight(), getSolidColor()));
    }
    if (!shapes.isEmpty()) {
      sb.append(prefix).append("\tShapes:\n");
      for (Object shape : shapes) {
        sb.append(prefix).append("\t\t").append(shape).append("\n");
      }
    }
    return sb.toString();
  }
}