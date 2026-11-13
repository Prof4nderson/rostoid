# Modelos faciais locais

O app carrega os modelos do pacote `@vladmandic/face-api` versão 1.7.15 a partir desta pasta local; nenhum modelo é buscado de CDN durante o uso. Os pesos incluídos são apenas modelos matemáticos e não contêm fotografias dos participantes.

Arquivos necessários: `tiny_face_detector_model-weights_manifest.json` e `tiny_face_detector_model.bin`; `face_landmark_68_model-weights_manifest.json` e `face_landmark_68_model.bin`; `face_recognition_model-weights_manifest.json` e `face_recognition_model.bin`. Os nomes e caminhos dentro dos manifestos devem permanecer iguais.

Origem: pacote publicado em https://unpkg.com/@vladmandic/face-api@1.7.15/model/ e repositório oficial https://github.com/vladmandic/face-api (licença MIT). Sem os manifestos e pesos, a interface explica que o reconhecedor está indisponível e não simula resultados.
