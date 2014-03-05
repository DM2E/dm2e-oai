use strict;
use warnings;
use Data::Dumper;
use Net::OAI::Harvester;
# use File::Slurp qw/read_file/;
use Time::HiRes;

my $baseURL = 'http://data.dm2e.eu/dm2e-oai/oai';

print "Harvesting from $baseURL\n";
## create a harvester for the Library of Congress
my $harvester = Net::OAI::Harvester->new( 
    'baseURL' => $baseURL,
);

## list all the records in a repository
my $records = $harvester->listAllRecords(
    'metadataPrefix'    => 'oai_dc' 
);
my $recordnumber = 0;
my $start = Time::HiRes::gettimeofday();
while ( my $record = $records->next() ) {
    my $elapsed = (Time::HiRes::gettimeofday() - $start) * 1000;
    my $header = $record->header();
    my $metadata = $record->metadata();
    print "identifier: ", $header->identifier(), "\n";
    print "title: ", $metadata->title(), "\n";
    print "Harvesting record # " . $recordnumber . "took $elapsed ms.\n";
    $start = Time::HiRes::gettimeofday(); 
    $recordnumber++;
}
print "Harvested $recordnumber records.";
