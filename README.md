# Security Research

This repo contains the code/data from multiple projects produced by the Security Research team at ServiceNow. The associated blog posts of these project can be found [here](https://securitylab.servicenow.com/research/).

## Adding Projects

Each project added should be in its own directory. If the project being added is contained within its own git repo and all files within the repo can be published publicly, use `git subtree` to add the project to this repo. This allows any changes to the project be easily pushed to this repo in the future. Otherwise, manually create the directory and copy the files over to this repo.

```bash
## Add a new project to this repo using subtree

# Add the project repo as a remote for this repo
git remote add -f [remote-name] [git@remote-path.git]
# Add the project at [remote-name] [remote-branch] to this repo under the folder [folder-name]
# This is will only include files from [remote-name] [remote-branch], no commits will be copied over
git subtree add --prefix [folder-name] [remote-name] [remote-branch] --squash

## Update an existing project in this repo that was added using subtree

# Grab the new data from [remote-name] [remote-branch]
git fetch [remote-name] [remote-branch]
# Merge the project at [remote-name] [remote-branch] to this repo under the folder [folder-name]
# This is will only include files from [remote-name] [remote-branch], no commits will be copied over
git subtree pull --prefix [folder-name] [remote-name] [remote-branch] --squash
```