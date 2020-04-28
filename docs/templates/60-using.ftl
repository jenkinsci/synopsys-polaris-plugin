# Using plugin in a build

You can use ${solution_name} in a freestyle build or a pipeline build.

## Freestyle build

To use ${solution_name} in a freestyle build, add it as a build step at the point where you want to build your project, and complete the following:

1. *Polaris CLI Installation*: Select the name that you assigned to your ${polaris_cli_name} installation on the *Manage Jenkins > Global Tool Configuration* page.
1. *Polaris Arguments*: The arguments that you want passed to the ${polaris_cli_name}, for example, *analyze*.
    Arguments that are passed in here must adhere to specific guidelines:
    * All arguments must be separated by whitespace (specifically: spaces, tabs, newlines, carriage returns, or linefeeds).
    * All values containing whitespace must be surrounded by quotes.
    * You must escape all quotes (", ') that are used in values.
    * You must escape all backslashes (\\) that are used in values.
1. *Wait for Issues*: If you want the build to wait until ${polaris_product_name} determines whether there are issues with your project, check this box and
use the *If there are issues* field to select the action you want the plugin to take when issues are discovered. Click *Advanced...* if you want to adjust maximum
length of time the job will wait for issues (*Job timeout in minutes*).
If you want the build to proceed without waiting, leave the *Wait for Issues* box unchecked.
1. Click *Save*.

## Pipeline build

The ${solution_name} plugin provides two pipeline steps:

* *polaris*: Runs the ${polaris_cli_name} to initiate ${polaris_product_name} analysis of your project.
* *polarisIssueCheck*: Waits until ${polaris_product_name} has completed analysis of your project, and determines the number of issues found.
    * Note that for polarisIssueCheck, the *timeout in minutes* field requires a positive integer. Values that are not integers may be truncated by Jenkins before being passed on to the plugin.

Documentation on using these pipeline steps can be found in the [Jenkins pipeline steps documentation](https://jenkins.io/doc/pipeline/steps/synopsys-polaris/).