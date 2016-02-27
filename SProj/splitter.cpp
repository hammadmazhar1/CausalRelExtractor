#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
using namespace std;

int main() {
	string str;
	ifstream reader("CompleteData.txt");
	int i = 1;

	while (getline(reader, str)) {
		stringstream sstr;
		sstr << "file" << i++ << ".txt";
		ofstream writer(sstr.str().c_str());
		writer << str;
		writer.close();
	}
	
	return 0;
}