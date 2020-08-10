<% def stats = utils.aggregateStats( data )
 %># Specification run results<% if (projectName && projectVersion) {
 %>

## Project: ${projectName}, Version: ${projectVersion} <%
  }
 %>

## Specifications summary

<small>Created on ${new Date()} by ${System.properties['user.name']}</small>

| Total          | Passed          | Failed          | Feature failures | Feature errors   | Success rate        | Total time (ms) |
|----------------|-----------------|-----------------|------------------|------------------|---------------------|-----------------|
| ${stats.total} | ${stats.passed} | ${stats.failed} | ${stats.fFails}  | ${stats.fErrors} | ${stats.successRate}| ${stats.time}   |

## Specifications

|Name  | Features | Failed | Errors | Skipped | Success rate | Time |
|------|----------|--------|--------|---------|--------------|------|
<% data.each { name, map ->
      def s = map.stats
 %>| $name | ${s.totalFeatures} | ${s.failures} | ${s.errors} | ${s.skipped} | ${s.successRate} | ${s.time} |
<% }
 %>