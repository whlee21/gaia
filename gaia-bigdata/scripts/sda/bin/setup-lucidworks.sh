# This creates the default external data source, just like the LucidWorksDataManagementService does when creating a new collection
$LWE_PYTHON/ds.py create name=collection1_SDA_DS type=external crawler=lucid.external source=SDA source_type=sda_document_service collection=collection1
