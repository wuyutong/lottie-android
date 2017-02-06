package com.airbnb.lottie;

import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonToken;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

abstract class BaseAnimatableValue<V, O>
    implements AnimatableValue<O>, AnimatableValueDeserializer<V> {
  List<Keyframe<V>> keyframes;
  final LottieComposition composition;
  private final boolean isDp;

  V initialValue;

  /** Create a default static animatable path. */
  BaseAnimatableValue(LottieComposition composition) {
    this.composition = composition;
    isDp = false;
  }

  BaseAnimatableValue(@Nullable JsonReader reader, LottieComposition composition,
      boolean isDp) throws IOException {
    this.composition = composition;
    this.isDp = isDp;
    if (reader != null) {
      init(reader);
    }
  }

  void init(JsonReader reader) throws IOException {
    boolean foundValue = false;
    while (reader.hasNext()) {
      switch (reader.nextName()) {
        case "k":
          foundValue = true;
          JsonToken value = reader.peek();
          if (value == JsonToken.BEGIN_ARRAY) {
            keyframes = Keyframe.parseKeyframes(reader, getScale(), this);
            if (!keyframes.isEmpty()) {
              initialValue = keyframes.get(0).startValue;
            }
          } else {
            initialValue = valueFromObject(reader, getScale());
          }
          break;
        default:
          reader.skipValue();
      }
    }
    if (!foundValue) {
      throw new IllegalArgumentException("Unable to parse animatable value.");
    }
  }

  private float getScale() {
    return isDp ? composition.getScale() : 1f;
  }

  private long getDurationFrames() {
    if (keyframes.isEmpty()) {
      throw new IllegalStateException("There are no keyframes.");
    }
    return keyframes.get(keyframes.size() - 1).startFrame - keyframes.get(0).startFrame;
  }

  long getDuration() {
    return (long) (getDurationFrames() / (float) composition.getFrameRate() * 1000);
  }

  long getDelay() {
    if (keyframes.isEmpty()) {
      return 0;
    }
    return (long) (keyframes.get(0).startFrame / (float) composition.getFrameRate() * 1000);
  }

  O convertType(V value) {
    //noinspection unchecked
    return (O) value;
  }

  public boolean hasAnimation() {
    return !keyframes.isEmpty();
  }

  public O getInitialValue() {
    return convertType(initialValue);
  }

  public abstract V valueFromObject(JsonReader reader, float scale) throws IOException;

  public abstract KeyframeAnimation<O> createAnimation();

  @Override public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("initialValue=").append(initialValue);
    if (!keyframes.isEmpty()) {
      sb.append(", values=").append(Arrays.toString(keyframes.toArray()));
    }
    return sb.toString();
  }
}