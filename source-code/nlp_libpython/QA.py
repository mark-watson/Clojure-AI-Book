from transformers import pipeline

qa = pipeline(
    "question-answering",
    #model="NeuML/bert-small-cord19qa",
    model="NeuML/bert-small-cord19-squad2",
    tokenizer="NeuML/bert-small-cord19qa"
)

def answer (query_text,context_text):
  if not context_text or not context_text.strip():
    result = {'score': 0.0, 'start': 0, 'end': 0, 'answer': ''}
    print(result)
    return result
  answer = qa({
                "question": query_text,
                "context": context_text
               })
  print(answer)
  return answer
