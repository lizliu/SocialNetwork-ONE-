#include <stdlib.h>
#include <stdio.h>
FILE *infile,*outfile;
FILE *infile2,*outfile2;
char filename1[30],filename2[30];
int main(){
	infile = fopen("infocom2006.txt", "r");
	outfile =  fopen("infocom2006-sorted.txt", "w"); 
	double time;
	char conn[10],updown[10];
	int from,to;
	int map[100];
	int i,counter=0;
	for(i=0;i<100;i++){
		map[i] = -1;
	}
	while(fscanf(infile, "%lf %s %d %d %s", &time,conn,&from,&to,updown) != EOF){
		if(updown[0]=='u'){
			if(map[from] == -1){
				map[from] = counter;
				counter++;
			}
			if(map[to] == -1){
				map[to] = counter;
				counter++;
			}
			fprintf(outfile,"%lf %s %d %d %s\n",time,conn,map[from],map[to],updown);
		}
		else if(updown[0]=='d'){
			if(map[from] == -1){
				map[from] = counter;
				counter++;
			}
			if(map[to] == -1){
				map[to] = counter;
				counter++;
			}
			fprintf(outfile,"%lf %s %d %d %s\n",time,conn,map[from],map[to],updown);
		}	
	}
	fclose(infile);
	fclose(outfile);
	printf("%d",counter);
	for(i=0;i<100;i++){
		if(map[i] != -1){
			sprintf( filename1, "%d.dat", i );
			infile2 = fopen(filename1, "r");
			sprintf( filename2, "C:\\Users\\Rifur\\Desktop\\Datasets\\65\\ee\\%d.dat", map[i] );
			outfile2 =  fopen(filename2, "w");
			char ch;                                      
			while((ch = fgetc(infile2)) != EOF) { 
				fputc(ch, outfile2);
			} 
			fclose(infile2);
			fclose(outfile2);
		}
	}
	return 0;
}

