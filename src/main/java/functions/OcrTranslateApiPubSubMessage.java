/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package functions;

// [START functions_ocr_translate_pojo]

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

// Object for storing OCR translation requests
public class OcrTranslateApiPubSubMessage {
  private static final Gson gson = new Gson();

  private String kind;
  private String id;
  private String selfLink;
  private String name;
  private String bucket;
  private String generation;
  private String metageneration;
  private String contentType;
  private String timeCreated;
  private String updated;
  private String storageClass;
  private String timeStorageClassUpdated;
  private String size;
  private String md5Hash;
  private String mediaLink;
  private String crc32c;
  private String etag;

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSelfLink() {
    return selfLink;
  }

  public void setSelfLink(String selfLink) {
    this.selfLink = selfLink;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getGeneration() {
    return generation;
  }

  public void setGeneration(String generation) {
    this.generation = generation;
  }

  public String getMetageneration() {
    return metageneration;
  }

  public void setMetageneration(String metageneration) {
    this.metageneration = metageneration;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getTimeCreated() {
    return timeCreated;
  }

  public void setTimeCreated(String timeCreated) {
    this.timeCreated = timeCreated;
  }

  public String getUpdated() {
    return updated;
  }

  public void setUpdated(String updated) {
    this.updated = updated;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  public String getTimeStorageClassUpdated() {
    return timeStorageClassUpdated;
  }

  public void setTimeStorageClassUpdated(String timeStorageClassUpdated) {
    this.timeStorageClassUpdated = timeStorageClassUpdated;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getMd5Hash() {
    return md5Hash;
  }

  public void setMd5Hash(String md5Hash) {
    this.md5Hash = md5Hash;
  }

  public String getMediaLink() {
    return mediaLink;
  }

  public void setMediaLink(String mediaLink) {
    this.mediaLink = mediaLink;
  }

  public String getCrc32c() {
    return crc32c;
  }

  public void setCrc32c(String crc32c) {
    this.crc32c = crc32c;
  }

  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  public OcrTranslateApiPubSubMessage(String kind, String id, String selfLink, String name, String bucket, String generation
          , String metageneration, String contentType, String timeCreated, String updated, String storageClass
          , String timeStorageClassUpdated, String size, String md5Hash, String mediaLink, String crc32c, String etag) {

    this.kind = kind;
    this.id = id;
    this.selfLink = selfLink;
    this.name = name;
    this.bucket = bucket;
    this.generation = generation;
    this.metageneration = metageneration;
    this.contentType = contentType;
    this.timeCreated = timeCreated;
    this.updated = updated;
    this.storageClass = storageClass;
    this.timeStorageClassUpdated = timeStorageClassUpdated;
    this.size = size;
    this.md5Hash = md5Hash;
    this.mediaLink = mediaLink;
    this.crc32c = crc32c;
    this.etag = etag;
  }

  public static OcrTranslateApiPubSubMessage fromPubsubData(byte[] data) {
    String jsonStr = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
    Map<String, String> jsonMap = gson.fromJson(jsonStr, Map.class);

    return new OcrTranslateApiPubSubMessage(
        jsonMap.get("kind"), jsonMap.get("id"), jsonMap.get("selfLink"), jsonMap.get("name"), jsonMap.get("bucket")
            , jsonMap.get("generation"), jsonMap.get("metageneration"), jsonMap.get("contentType"), jsonMap.get("timeCreated")
            , jsonMap.get("updated"), jsonMap.get("storageClass"), jsonMap.get("timeStorageClassUpdated"), jsonMap.get("size")
            , jsonMap.get("md5Hash"), jsonMap.get("mediaLink"), jsonMap.get("crc32c"), jsonMap.get("etag")
    );
  }

  public byte[] toPubsubData() {
    return gson.toJson(this).getBytes(StandardCharsets.UTF_8);
  }
}
// [END functions_ocr_translate_pojo]