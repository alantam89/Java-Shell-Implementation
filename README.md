# Java-Shell-Implementation

This program was a project from Ravi Narayan's 332 Operating Systems class. It uses the ProcessBuilder class to handle shell commands.
This program enhances the Processbuilder class to use the following additional commands: cd, pwd, |, jobs, &, fg.
you can test the pipe function with the following example:
cat Jsh.java | grep private | grep String

To test the & and fg functions I'd recommend using the sleep command in the following order:
sleep 30 &
sleep 20 &
jobs
fg 1
fg 2

The above commands will put the sleep commands in the background, show the number of jobs, and bring them into the foreground.
The process will be evident by the terminal being in suspension for 30/20 seconds from initial call.
The program has an interactive input so you may type ctrl + d to exit.
