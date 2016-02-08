import re;

search_phrases_causal = ['because', 'for this reason', 'for that reason', 'consequently', 'as a consequence of', 'as a result of'];
search_phrases_non_causal = ['but', 'in short', 'in other words', 'whereas', 'on the other hand', 'nevertheless', 'nonetheless', 'in spite of', 'in contrast', 'however', 'even', 'though', 'despite the fact', 'conversely', 'although'];

search_phrases_causal_sentences = {'because':[], 'for this reason':[], 'for that reason':[], 'consequently':[], 'as a consequence of':[], 'as a result of':[]};

with open('tinyfile.txt', 'r') as myfile:
    sentences = myfile.read().replace('\n', '').lower().split('. ');

# all sentences
for str in sentences:
	print str;

for str in search_phrases_causal:
	for sentence in sentences:
		if str in sentence:
			search_phrases_causal_sentences[str].append(sentence);

for str in search_phrases_causal:
	if search_phrases_causal_sentences[str]:
		print "\n" + str;
		for sentence in search_phrases_causal_sentences[str]:
			print "\t" + sentence;
