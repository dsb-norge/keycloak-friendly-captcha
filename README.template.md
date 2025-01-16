# Template README

This template project is used to scaffold new github projects under dsb-norge.

In the scaffolded project based on this template, do the following:
1. Rename `.rename_me_as_dot_github` to `.github`
2. Check the contents of the workflow files under `.github/workflows`, adjust as necessary:
   1. `autobump-deps.yml`: You could set a different time for the job to run. If several apps use the same scheduling, jobs will be queued - this should not be a problem, but scheduling at different times does not hurt. Just make sure runners are available at the time you schedule!
   2. `ci-cd.yml`: Defaults works fine for normal apps where frontend and/or backend should be deployed to dev/test and PRs should get ephemeral environments. If this does not suit your project, you may need to adjust the file.
3. When you are done scaffolding, this README should be turned into a normal app README 😉
