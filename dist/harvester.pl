use Net::OAI::Harvester;
use File::Slurp qw/read_file/;
use strict;
use warnings;
use Data::Dumper;
# my $baseURL = 'http://141.20.126.232/dm2e-oai/oai';
my $baseURL = 'http://localhost:7777/oai';

## create a harvester for the Library of Congress
my $harvester = Net::OAI::Harvester->new( 
    'baseURL' => $baseURL,
);

## list all the records in a repository
my $records = $harvester->listAllRecords(
    'metadataPrefix'    => 'oai_dc' 
);
while ( my $record = $records->next() ) {
    my $header = $record->header();
    my $metadata = $record->metadata();
    print "identifier: ", $header->identifier(), "\n";
    print "title: ", $metadata->title(), "\n";
}
