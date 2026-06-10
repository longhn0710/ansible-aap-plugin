# Ansible Automation Platform Plugin

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/ansible-aap.svg)](https://plugins.jenkins.io/ansible-aap)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/ansible-aap-plugin.svg?label=changelog)](https://github.com/jenkinsci/ansible-aap-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/ansible-aap.svg?color=blue)](https://plugins.jenkins.io/ansible-aap)

This plugin connects Jenkins to Red Hat Ansible Automation Platform (AAP) and compatible AWX/Ansible Tower controllers.

It can:

* Launch job templates and workflow job templates from Jenkins.
* Import AAP job output into the Jenkins console.
* Import workflow child job output.
* Update projects and set project revisions.
* Return exported values from Ansible jobs back to Jenkins pipelines.

This plugin is based on the Jenkins Ansible Tower plugin and uses a separate plugin ID and Pipeline step names so it can be installed alongside the existing `ansible-tower` plugin.

## Compatibility

The plugin supports both legacy AWX/Ansible Tower UI URLs and AAP Controller UI URLs.

For legacy Tower/AWX, job links use the historical format:

```text
https://controller.example.com/#/jobs/123
https://controller.example.com/#/workflows/456
```

For AAP Controller, job links use the current execution output format:

```text
https://controller.example.com/execution/jobs/playbook/123/output
https://controller.example.com/execution/jobs/workflow/456/output
https://controller.example.com/execution/jobs/project_update/789/output
```

If your AAP API gateway and AAP Controller UI use different host names, configure both `URL` and `Display URL` as described below.

## Installation

Build the plugin with Maven:

```bash
mvn test
mvn package
```

The packaged plugin is generated at:

```text
target/ansible-aap.hpi
```

Install the `.hpi` file from:

```text
Manage Jenkins -> Plugins -> Advanced settings -> Deploy Plugin
```

Restart Jenkins if required.

## Global Configuration

After installing the plugin, open:

```text
Manage Jenkins -> System -> Ansible Automation Platform
```

Add one or more AAP installations.

| Field | Description |
| --- | --- |
| Name | Logical name used by Pipeline steps through `aapServer`. |
| URL | Base API URL for AAP, AWX, or Tower. For AAP gateway deployments this is usually the gateway host. |
| Display URL | Optional UI base URL used when printing links to job output. Set this when the API URL and Controller UI URL are different. |
| Credentials | Jenkins credentials used to authenticate with AAP. Use "Username with password" for basic authentication, or "Secret text" for token-based authentication where supported. |
| Force Trust Cert | Trust the HTTPS certificate even if Jenkins does not trust the issuer. Use carefully, mainly for internal or test environments. |
| Enable Debugging | Write detailed API/debug messages to Jenkins logs. Enable only while troubleshooting. |

Example AAP 2.x style configuration:

```text
Name:        production-aap
URL:         https://aap.example.com
Display URL: https://aap-controller.example.com
Credentials: jenkins-aap-service-account
```

If `Display URL` is not set, the plugin uses `URL` as the UI base. For AAP Controller deployments this may produce links to the API gateway instead of the Controller UI, so setting `Display URL` is recommended when those hosts differ.

## Pipeline Usage

The main Pipeline step is `ansibleAAP`.

Minimal job template example:

```groovy
ansibleAAP(
    aapServer: 'production-aap',
    jobTemplate: 'Deploy application',
    templateType: 'job'
)
```

Workflow template example:

```groovy
ansibleAAP(
    aapServer: 'production-aap',
    jobTemplate: '329',
    jobType: 'run',
    templateType: 'workflow',
    aapCredentialsId: 'jenkins-aap-service-account',
    aapLogLevel: 'full',
    importWorkflowChildLogs: true,
    throwExceptionWhenFail: true,
    extraVars: '''---
environment: develop
application_name: payment-service
image_tag: 1.0.0
'''
)
```

Common parameters:

| Parameter | Description |
| --- | --- |
| `aapServer` | Required. Name of the AAP installation from Jenkins global configuration. |
| `jobTemplate` | Required. Template name or numeric template ID. |
| `templateType` | `job` or `workflow`. Defaults to `job`. |
| `jobType` | `run` or `check`, depending on template settings. |
| `aapCredentialsId` | Optional credential override. If empty, the global installation credential is used. |
| `aapLogLevel` | Output import mode. Common values are `false`, `true`, `full`, and `variables`. |
| `extraVars` | YAML extra variables passed to AAP. |
| `inventory` | Inventory name or ID override. |
| `credential` | Credential name or ID override for the launched AAP job. |
| `limit` | Host limit. |
| `jobTags` | Job tags. |
| `skipJobTags` | Skip tags. |
| `scmBranch` | SCM branch override. |
| `verbose` | Print additional Jenkins console messages. |
| `removeColor` | Strip ANSI color from imported AAP output. |
| `importWorkflowChildLogs` | Import child job output for workflow templates. |
| `throwExceptionWhenFail` | Fail the Jenkins build when the AAP job fails. Defaults to `true`. |
| `async` | Launch the job and return immediately. Defaults to `false`. |

For non-async job runs, the step returns a map-like object with:

| Key | Description |
| --- | --- |
| `JOB_ID` | AAP job ID. |
| `JOB_URL` | UI URL for the launched job. |
| `JOB_RESULT` | `SUCCESS` or `FAILED`. |

Example:

```groovy
def result = ansibleAAP(
    aapServer: 'production-aap',
    jobTemplate: 'Deploy application',
    templateType: 'job',
    aapLogLevel: 'full',
    throwExceptionWhenFail: false
)

echo "AAP job ${result.JOB_ID}: ${result.JOB_URL}"
echo "AAP result: ${result.JOB_RESULT}"
```

## Project Sync

The Pipeline step for project updates is `ansibleAAPProjectSync`.

```groovy
def syncResult = ansibleAAPProjectSync(
    aapServer: 'production-aap',
    project: 'Infrastructure project',
    aapCredentialsId: 'jenkins-aap-service-account',
    importAAPLogs: true,
    throwExceptionWhenFail: true
)

echo "AAP project sync ${syncResult.SYNC_ID}: ${syncResult.SYNC_URL}"
```

Common parameters:

| Parameter | Description |
| --- | --- |
| `aapServer` | Required. Name of the AAP installation. |
| `project` | Required. Project name or ID. |
| `aapCredentialsId` | Optional credential override. |
| `importAAPLogs` | Import sync output into Jenkins. |
| `verbose` | Print additional Jenkins console messages. |
| `removeColor` | Strip ANSI color from imported output. |
| `throwExceptionWhenFail` | Fail the Jenkins build when sync fails. |
| `async` | Start the sync and return immediately. |

## Project Revision

The Pipeline step for setting a project revision is `ansibleAAPProjectRevision`.

```groovy
ansibleAAPProjectRevision(
    aapServer: 'production-aap',
    project: 'Infrastructure project',
    revision: 'main',
    aapCredentialsId: 'jenkins-aap-service-account',
    throwExceptionWhenFail: true
)
```

## Freestyle Jobs

The plugin also provides Freestyle build steps:

* `Ansible Automation Platform`
* `Ansible Automation Platform Project Sync`
* `Ansible Automation Platform Project Revision`

The Freestyle fields map to the Pipeline parameters documented above.

## Migration from Ansible Tower Plugin

This plugin is intentionally separate from the existing `ansible-tower` plugin.

Use the new Jenkins global configuration section:

```text
Manage Jenkins -> System -> Ansible Automation Platform
```

Pipeline syntax changes:

| Old `ansible-tower` syntax | New `ansible-aap` syntax |
| --- | --- |
| `ansibleTower` | `ansibleAAP` |
| `ansibleTowerProjectSync` | `ansibleAAPProjectSync` |
| `ansibleTowerProjectRevision` | `ansibleAAPProjectRevision` |
| `towerServer` | `aapServer` |
| `towerCredentialsId` | `aapCredentialsId` |
| `towerLogLevel` | `aapLogLevel` |
| `importTowerLogs` | `importAAPLogs` |

Before:

```groovy
ansibleTower(
    towerServer: 'legacy-tower',
    towerCredentialsId: 'jenkins-tower-credential',
    jobTemplate: '329',
    jobType: 'run',
    templateType: 'workflow',
    towerLogLevel: 'full'
)
```

After:

```groovy
ansibleAAP(
    aapServer: 'production-aap',
    aapCredentialsId: 'jenkins-aap-credential',
    jobTemplate: '329',
    jobType: 'run',
    templateType: 'workflow',
    aapLogLevel: 'full'
)
```

The plugin IDs and configuration files are different, so both plugins can be installed on the same Jenkins controller while jobs are migrated.

## Authentication

The plugin can authenticate with a username/password credential. When username/password authentication is used, the plugin attempts token-based API authentication where supported and falls back according to controller capabilities.

For token-based authentication, use a Jenkins "Secret text" credential if supported by your AAP/Tower version and security model.

Use a dedicated service account with the minimum permissions required to:

* Read job and workflow templates.
* Launch the required templates.
* Read launched jobs and job events.
* Read project sync status when using project sync.

## Importing Output

Use `aapLogLevel` to control how much AAP output is copied into the Jenkins console.

Common modes:

| Value | Behavior |
| --- | --- |
| `false` | Do not import AAP output. |
| `true` | Import truncated output similar to the AAP UI. |
| `full` | Import full non-truncated event output. |
| `variables` | Process exported variables without printing logs. |

For workflow templates, set `importWorkflowChildLogs: true` to include child job output.

## Returning Data to Jenkins

The plugin recognizes exported values from Ansible output and returns them to Jenkins.

Purpose-driven logging example:

```yaml
- name: Export a Jenkins variable
  debug:
    msg: "JENKINS_EXPORT RELEASE_VERSION=1.2.3"
```

`set_stats` example:

```yaml
- name: Export values with set_stats
  set_stats:
    data:
      JENKINS_EXPORT:
        - release_version: "1.2.3"
        - deployment_status: "ok"
    aggregate: yes
    per_host: no
```

Pipeline example:

```groovy
def result = ansibleAAP(
    aapServer: 'production-aap',
    jobTemplate: 'Deploy application',
    templateType: 'job',
    aapLogLevel: 'variables'
)

echo "Release version: ${result.release_version}"
```

## Async Execution

Set `async: true` to launch an AAP job and return control to the Pipeline immediately.

```groovy
def launched = ansibleAAP(
    aapServer: 'production-aap',
    jobTemplate: 'Long running deployment',
    templateType: 'job',
    async: true
)

echo "AAP job ${launched.JOB_ID} was submitted: ${launched.JOB_URL}"
```

Async jobs return an object that can be used by trusted Pipeline code to check status and fetch logs. Depending on your Jenkins script security configuration, additional in-process script approvals may be required.

## Development

Build and test locally:

```bash
mvn test
mvn package
```

Run a specific test:

```bash
mvn -Dtest=AAPConnectorDisplayUrlTest test
```

If you are using an older Maven version locally and the Jenkins plugin parent enforcer blocks the build, upgrade Maven to the required version. During local experimentation only, you can bypass the enforcer:

```bash
mvn -Denforcer.skip=true test
mvn -Denforcer.skip=true package
```

## License

This plugin is released under the MIT License. See [LICENSE](LICENSE).
