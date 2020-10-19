# Using plugin in a build

You can use ${solution_name} in either freestyle or pipeline jobs to invoke the ${polaris_cli_name} as part of your Jenkins build.
Refer to the [Polaris Command Line Reference](https://sig-docs.synopsys.com/polaris/docs/c_cli-overview) for information about the Polaris CLI.

## Freestyle job

To use ${solution_name} in a freestyle job, add it as a build step at the point where you want to build your project, and complete the following:

1. *Polaris CLI Installation*: Select the name that you assigned to your ${polaris_cli_name} installation on the *Manage Jenkins > Global Tool Configuration* page.
1. *Polaris Arguments*: The arguments that you want passed to the ${polaris_cli_name}, for example, *analyze*.
1. *Wait for Issues*: If you want the build to wait until ${polaris_product_name} determines whether there are issues with your project, check this box and
use the *If there are issues* field to select the action you want the plugin to take when issues are discovered. Click *Advanced...* if you want to adjust maximum
length of time the job will wait for issues (*Job timeout in minutes*).<br/>
If you want the build to proceed without waiting, leave the *Wait for Issues* box unchecked.<br/>
**Please note:** *Wait for Issues* will not cause the build to fail based on the results of incremental analysis, but on the results of issues in ${polaris_product_name}.
1. Click *Save*.

### Limited Customer Availability Features

For customers using Incremental Analysis (currently in LCA), ${solution_name} offers the ability to create a changeset file for use with Incremental Analysis.
Use the generated changeset file with incremental analysis (--incremental) by passing it in as a part of your *Polaris Arguments* as $CHANGE_SET_FILE_PATH.

1. *Populate SCM changeset in file at $CHANGE_SET_FILE_PATH for incremental analysis*: Checking this box will cause the plugin to generate a changeset file at
$CHANGE_SET_FILE_PATH (by default, $WORKSPACE/.synopsys/polaris/changeSetFile.txt) based on the changeset reported to Jenkins from the SCM filtered by the configured
inclusion and exclusion patterns. If no files would be included in the changeset file, skips analysis and sets the configured status.
    1. *Changeset inclusion patterns*: If this field is not blank, *only* files matching this wildcard filepattern will be included in the changeset file. If this
field is blank, all files will be included in the changeset file.
    1. *Changeset exclusion patterns*: Any files matching this wildcard filepattern will be excluded from the changeset file. If this field is blank, no files will be
excluded.
    1. *When static analysis is skipped because the changeset contained no files to analyze*: The build status to set when analysis was skipped because the changeset
contained no files to analyze.

## Pipeline job

The ${solution_name} plugin provides two pipeline steps:

* *polaris*: Runs the ${polaris_cli_name} to initiate ${polaris_product_name} analysis of your project.
* *polarisIssueCheck*: Waits until ${polaris_product_name} has completed analysis of your project, and determines the number of issues found.
    * Note that for polarisIssueCheck, the *timeout in minutes* field requires a positive integer. Non-integer values may be truncated by Jenkins before being passed on to the plugin.

Documentation on using these pipeline steps can be found in the [Jenkins pipeline steps documentation](https://jenkins.io/doc/pipeline/steps/synopsys-polaris/).

## Escaping arguments

Whether using ${solution_name} in a freestyle or pipeline job, ${polaris_cli_name} arguments must adhere to specific guidelines:

* All arguments must be separated by whitespace (specifically: spaces, tabs, newlines, carriage returns, or linefeeds).
* All values containing whitespace must be surrounded by quotation marks.
* You must escape all quotation marks (", ') that are used in values.
* You must escape all backslashes (\\) that are used in values.

For example, the following arguments:

`--co project={"branch":"new_branch", "name":"new_name"} analyze`

are escaped to:

`--co project='{"branch":"new_branch", "name":"new_name"}' analyze`
