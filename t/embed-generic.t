#!perl

use 5.006;
use strict;
use warnings;

use File::Copy qw(copy);
use File::Spec::Functions qw(catfile);
use File::Temp qw(tempdir);
use FindBin;
use Test::More tests => 6;

my $tmp = tempdir(CLEANUP => 1);
note ("I am placing blocks because I'm in " . $tmp);

system './gen-embed "BinarySearchTree<String,Integer>"';
cmp_ok (0, '==', $? >> 8, './gen-embed exits normal');

open my $fi, '<', 'import.java';
ok (defined $fi, 'import.java readable') or diag "error: $!";
open my $fe, '<', 'embed.java';
ok (defined $fe, 'embed.java readable') or diag "error: $!";

my $src;
my $src_BST = catfile ($FindBin::Bin, "src/BinarySearchTree.java");
unless (open $src, '<', $src_BST) {
	my $flw = "FATAL: missing $src_BST: $!";
	close $fe;
	close $fi;
	die $flw;
}

my $dst;
my $tmp_BST = catfile ($tmp, "BinarySearchTree.java");
unless (open $dst, '>', $tmp_BST) {
	my $flw = "FATAL: cannot open $tmp_BST: $!";
	close $src;
	close $fe;
	close $fi;
	die $flw;
}

while (<$src>) {
	if (m!^//import.java//$! && defined $fi) {
		print $dst $_ while <$fi>;
	}
	elsif (m!^//embed.java//$! && defined $fe) {
		print $dst $_ while <$fe>;
	}
	else {
		print $dst $_;
	}
}

close $dst;
close $src;
close $fe;
close $fi;

# note ("$tmp_BST:");
# note (cat ($tmp_BST));

system "javac -Xlint:all -d $tmp -cp $tmp $tmp_BST";
cmp_ok ($? >> 8, '==', 0, 'javac exits normal');

my $out = `java -cp $tmp BinarySearchTree 1..1`;
cmp_ok ($? >> 8, '==', 0, 'main program execs normal');
is ($out, <<'EOF', 'main program looks normal');
1..11
# Start subtest 1
ok 1 - A 8
ok 2 - C 4
ok 3 - E 12
ok 4 - H 5
ok 5 - L 11
ok 6 - M 9
ok 7 - P 10
ok 8 - R 3
ok 9 - S 0
ok 10 - X 7
ok 11 - Iterator exhausted
# End of subtest 1
# Ran 11 tests and failed 0 tests.
EOF

sub cat
{
	open my $fh, '<', shift	or return "error: $!";
	my @lines = <$fh>;
	close $fh;
	return wantarray ? @lines : "@lines";
}
