#include <stdlib.h>
#include <stdio.h>
FILE *infile,*outfile;
char filename1[30],filename2[30];
int main(){
	infile = fopen("haggle6-infocom6.csv", "r");
	outfile =  fopen("infocom2006.txt", "w"); 
	double time;
	char conn[10],updown[10];
	int from,to;
	while(fscanf(infile, "%lf %s %d %d %s", &time,conn,&from,&to,updown) != EOF){
		from++;
		to++;
		if( 20 < from && 20 < to &&  from <= 99 && to <= 99 ){
			if(from != 29 && from != 33 && from != 38 && from != 47 && from != 50 && from != 58 && from != 61 && from != 63 && from != 78 && from != 79 && from != 85 && from != 90 && from != 91 && from != 94){
				if(to != 29 && to != 33 && to != 38 && to != 47 && to != 50 && to != 58 && to != 61 && to != 63 && to != 78 && to != 79 && to != 85 && to != 90 && to != 91 && to != 94){
					if(updown[0]=='u'){
						fprintf(outfile,"%lf %s %d %d %s\n",time,conn,from,to,updown);
					}
					else if(updown[0]=='d'){
						fprintf(outfile,"%lf %s %d %d %s\n",time,conn,from,to,updown);
					}
				}
			}
		}			
	}
	fclose(infile);
	fclose(outfile);
	return 0;
}

