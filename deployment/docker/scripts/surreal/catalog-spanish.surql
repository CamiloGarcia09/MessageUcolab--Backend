-- Aplicación opcional y explícita sobre una base existente de MessageUcoLab.
-- Solo cambia etiquetas de registros de catálogo conocidos. No crea ni elimina datos.
-- No ejecutar el seed completo surreal-init.surql como sustituto de este archivo.
USE NS messageucolab;
USE DB messageucolab;

BEGIN TRANSACTION;

UPDATE language_base:`6319ca87-47b3-4e0e-994f-3166378988f2` SET language = "Inglés";
UPDATE language_base:`bed4d574-4c7f-4d95-bc12-6dbbcdbbf4fc` SET language = "Español";
UPDATE language_base:`8cc7ae54-db8c-4ef1-929b-22e17de58129` SET language = "Francés";
UPDATE application_state:`dfb7a7cb-66f6-4b73-a1d8-803336f72ebe` SET name = "Activo";
UPDATE application_state:`2a0ddacd-2dcc-47e2-b56f-03db5207c443` SET name = "Inactivo";
UPDATE environment_type:`3ba48618-abb9-40c2-b700-e4413dd9332b` SET name = "Develop";
UPDATE environment_type:`71ff34c1-1f4b-4858-943e-771907071c02` SET name = "Produccion";
UPDATE environment_type:`19b374bd-503a-4e65-b729-4c2442a69a4e` SET name = "Testing";
UPDATE environment_state:`e6e00788-eda8-40fd-aa31-5763133e8834` SET name = "Activo";
UPDATE environment_state:`ec30283f-2b63-485e-ab7a-5bd6f44c8878` SET name = "Inactivo";
UPDATE functionality_state:`407de974-c8aa-43e2-a5cf-22dc7c44054b` SET name = "Activo";
UPDATE functionality_state:`505cde87-96f2-4926-8dc6-b04529469bd3` SET name = "Inactivo";
UPDATE message_state:`4223b3dc-c991-4603-a746-d37ce5ecd976` SET name = "Activo";
UPDATE message_state:`5861f44a-1303-41dc-a079-a7bd7c5cfc47` SET name = "Inactivo";
UPDATE message_type:`0c71601d-96b3-417e-b385-06b10ba126d9` SET name = "Funcional";
UPDATE message_type:`d6cca1fe-1801-48c7-98ce-0e63255d187e` SET name = "Técnico";
UPDATE message_category:`c1e02ac3-02c3-4818-8f94-f35d852db9b0` SET name = "Error";
UPDATE message_category:`3b337f14-dde1-436e-829a-eebacb556eb4` SET name = "Información";
UPDATE message_category:`4276d9a3-c403-4c35-9167-97da025a2fd5` SET name = "Confirmación";
UPDATE message_category:`9eaca010-c577-4af0-8b05-4a0a82450311` SET name = "Advertencia";
UPDATE message_category:`bf416ec8-64a9-43a4-b3db-0c8a79ba525f` SET name = "Depuración";
UPDATE message_environment_state:`10ddbab2-352e-48de-b9eb-de0895ddb944` SET name = "Activo";
UPDATE message_environment_state:`e68930ad-772a-4d38-beae-b20434372957` SET name = "Inactivo";

COMMIT TRANSACTION;
