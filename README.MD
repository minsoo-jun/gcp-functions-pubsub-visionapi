gcloud functions deploy ocr-translate \
--entry-point functions.OcrTranslateText \
--runtime java11 \
--memory 512MB \
--region asia-northeast1 \
--trigger-topic image-upload  \
--set-env-vars "^:^GCP_PROJECT=minsoojunprj:RESULT_TOPIC=trans_res:TO_LANG=ko,en"