import os;

for filename in os.listdir(os.getcwd()):
	f = open(filename, 'r')
	text = f.read()
	f.close()

	f = open(filename+"2", 'w')
	f.write(text)
	f.close()