default files = FLUX_DIR;
 // the input files are not provided here, 
//  see https://github.com/lobid/lodmill/blob/master/lodmill-rd/doc/zvdd/harvestOAI_hbz_zvdd.sh
 //  how to obtain them
default input ="../../../../../"; 
files+input + "/zvdd.xml"|
open-file |
decode-xml |
handle-marcxml |
morph(files + "morph_zvdd-title-digital2ld.xml") |
encode-ntriples |
write(files+"zvdd-title-digital_output.nt");