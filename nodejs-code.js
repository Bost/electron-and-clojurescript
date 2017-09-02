// node.js child_process exampless:
/*
var exec = require('child_process').exec;
exec('pwd', function callback(error, stdout, stderr) {
    console.log(stdout);
});
*/

var spawn = require('child_process').spawn;
var prc =
    // spawn('java', ['-jar', '-Xmx512M', '-Dfile.encoding=utf8', 'script/importlistings.jar']);
    // spawn('java', ['-cp', '~/.m2/repository/org/clojure/clojure/1.8.0/clojure-1.8.0.jar', 'clojure.main']);
    spawn('lein', ['repl']);

//noinspection JSUnresolvedFunction
prc.stdout.setEncoding('utf8');
prc.stdout.on('data', function (data) {
    var str = data.toString()
    var lines = str.split(/(\r?\n)/g);
    console.log(lines.join(""));
});

prc.on('close', function (code) {
    console.log('process exit code ' + code);
});
