import chromadb

# Initialize a local persistent database
client = chromadb.PersistentClient(path="./chroma_db")

def add_documents(collection_name, documents, metadatas, ids):
    """
    Add a collection of documents with metadata and unique IDs to the vector database.
    """
    collection = client.get_or_create_collection(name=collection_name)
    # Ensure lists are passed as standard python lists (libpython-clj passes them as iterable collections)
    collection.add(
        documents=list(documents),
        metadatas=list(metadatas),
        ids=list(ids)
    )
    return True

def query_documents(collection_name, query_text, n_results=2):
    """
    Perform semantic search and return a list of maps containing id, document, metadata, and distance.
    """
    collection = client.get_or_create_collection(name=collection_name)
    results = collection.query(
        query_texts=[query_text],
        n_results=int(n_results)
    )
    
    formatted = []
    if results and 'documents' in results and results['documents']:
        docs = results['documents'][0]
        metas = results['metadatas'][0]
        distances = results['distances'][0]
        ids = results['ids'][0]
        
        for i in range(len(docs)):
            formatted.append({
                "id": ids[i],
                "document": docs[i],
                "metadata": metas[i] if metas[i] else {},
                "distance": float(distances[i])
            })
    return formatted
