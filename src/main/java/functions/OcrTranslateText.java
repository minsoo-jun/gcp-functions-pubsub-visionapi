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

// [START functions_ocr_translate]

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.translate.v3.*;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.cloud.translate.v3.DetectLanguageRequest;
import com.google.cloud.translate.v3.DetectLanguageResponse;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageSource;
import functions.eventpojos.PubSubMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class OcrTranslateText implements BackgroundFunction<PubSubMessage> {
  // TODO<developer> set these environment variables
  private static final String PROJECT_ID = System.getenv("GCP_PROJECT");
  private static final String RESULTS_TOPIC_NAME = System.getenv("RESULT_TOPIC");
  private static final Logger LOGGER = Logger.getLogger(OcrTranslateText.class.getName());
  private static final String LOCATION_NAME = LocationName.of(PROJECT_ID, "global").toString();
  private static final String[] TO_LANGS = System.getenv("TO_LANG").split(",");

  private Publisher publisher;

  private List<OcrTranslateApiMessage> detectTextArray;

  public OcrTranslateText() throws IOException {
    publisher = Publisher.newBuilder(
            ProjectTopicName.of(PROJECT_ID, RESULTS_TOPIC_NAME)).build();
  }

  @Override
  public void accept(PubSubMessage pubSubMessage, Context context) {
    OcrTranslateApiPubSubMessage ocrMessage = OcrTranslateApiPubSubMessage.fromPubsubData(
            pubSubMessage.getData().getBytes(StandardCharsets.UTF_8));

    // call
    String bucket = ocrMessage.getBucket();
    String filename = ocrMessage.getName();
    LOGGER.info("Bucket: " + bucket + ", Filename: " + filename);
    detectText(bucket, filename);

    for (OcrTranslateApiMessage detectMessage : detectTextArray) {
      //TODO 디텍트후
      String targetLang = detectMessage.getLang();
      LOGGER.info("Translating text into " + targetLang);

      // Translate text to target language
      String text = detectMessage.getText();
      TranslateTextRequest request =
              TranslateTextRequest.newBuilder()
                      .setParent(LOCATION_NAME)
                      .setMimeType("text/plain")
                      .setTargetLanguageCode(targetLang)
                      .addContents(text)
                      .build();

      TranslateTextResponse response;
      try (TranslationServiceClient client = TranslationServiceClient.create()) {
        response = client.translateText(request);
      } catch (IOException e) {
        // Log error (since IOException cannot be thrown by a function)
        LOGGER.log(Level.SEVERE, "Error translating text: " + e.getMessage(), e);
        return;
      }
      if (response.getTranslationsCount() == 0) {
        return;
      }

      String translatedText = response.getTranslations(0).getTranslatedText();
      LOGGER.info("Translated text: " + translatedText);

      // Send translated text to (subsequent) Pub/Sub topic
      String detectedFilename = detectMessage.getFilename();
      OcrTranslateApiMessage translateMessage = new OcrTranslateApiMessage(
              translatedText, detectedFilename, targetLang);
      try {
        ByteString byteStr = ByteString.copyFrom(translateMessage.toPubsubData());
        PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();

        publisher.publish(pubsubApiMessage).get();
        LOGGER.info("Text translated to " + targetLang);
      } catch (InterruptedException | ExecutionException e) {
        // Log error (since these exception types cannot be thrown by a function)
        LOGGER.log(Level.SEVERE, "Error publishing translation save request: " + e.getMessage(), e);
      }
    }

  }

  private void detectText(String bucket, String filename) {
    LOGGER.info("Looking for text in image " + filename);

    List<AnnotateImageRequest> visionRequests = new ArrayList<>();
    String gcsPath = String.format("gs://%s/%s", bucket, filename);

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();

    Feature textFeature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
    AnnotateImageRequest visionRequest =
            AnnotateImageRequest.newBuilder().addFeatures(textFeature).setImage(img).build();
    visionRequests.add(visionRequest);

    // Detect text in an image using the Cloud Vision API
    AnnotateImageResponse visionResponse;
    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      visionResponse = client.batchAnnotateImages(visionRequests).getResponses(0);
      if (visionResponse == null || !visionResponse.hasFullTextAnnotation()) {
        LOGGER.info(String.format("Image %s contains no text", filename));
        return;
      }

      if (visionResponse.hasError()) {
        // Log error
        LOGGER.log(
                Level.SEVERE, "Error in vision API call: " + visionResponse.getError().getMessage());
        return;
      }
    } catch (IOException e) {
      // Log error (since IOException cannot be thrown by a Cloud Function)
      LOGGER.log(Level.SEVERE, "Error detecting text: " + e.getMessage(), e);
      return;
    }

    String text = visionResponse.getFullTextAnnotation().getText();
    LOGGER.info("Extracted text from image: " + text);

    // Detect language using the Cloud Translation API
    DetectLanguageRequest languageRequest =
            DetectLanguageRequest.newBuilder()
                    .setParent(LOCATION_NAME)
                    .setMimeType("text/plain")
                    .setContent(text)
                    .build();
    DetectLanguageResponse languageResponse;
    try (TranslationServiceClient client = TranslationServiceClient.create()) {
      languageResponse = client.detectLanguage(languageRequest);
    } catch (IOException e) {
      // Log error (since IOException cannot be thrown by a function)
      LOGGER.log(Level.SEVERE, "Error detecting language: " + e.getMessage(), e);
      return;
    }

    if (languageResponse.getLanguagesCount() == 0) {
      LOGGER.info("No languages were detected for text: " + text);
      return;
    }

    String languageCode = languageResponse.getLanguages(0).getLanguageCode();
    LOGGER.info(String.format("Detected language %s for file %s", languageCode, filename));

    // translate data set
    detectTextArray = new ArrayList();
    OcrTranslateApiMessage message;
    // Send a Pub/Sub translation request for every language we're going to translate to
    for (String targetLanguage : TO_LANGS) {

      LOGGER.info("Sending translation request for language " + targetLanguage);
      message = new OcrTranslateApiMessage(text, filename, targetLanguage);
      detectTextArray.add(message);
    }
  }
}
// [END functions_ocr_translate]
