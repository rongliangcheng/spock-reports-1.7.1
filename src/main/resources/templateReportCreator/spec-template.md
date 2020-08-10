<%
    def stats = utils.stats( data )
 %># Report for ${utils.getSpecClassName( data )}

##Summary

* Total Runs: ${stats.totalRuns}
* Success Rate: ${fmt.toPercentage(stats.successRate)}
* Failures: ${stats.failures}
* Errors:   ${stats.errors}
* Skipped:  ${stats.skipped}
* Total time: ${fmt.toTimeDuration(stats.time)}

<%
    def specTitle = utils.specAnnotation( data, spock.lang.Title )?.value()
    if ( specTitle ) {
        specTitle.split('\n').each { out << '##' << it << '\n' }
    }
    if ( data.info.narrative ) {
        if ( specTitle ) { out << '\n' }
        out << '<pre>\n' << data.info.narrative << '\n</pre>'
    }
    
    def writeTagOrAttachment = { feature ->
        def tagsByKey = feature.tags.groupBy( { t -> t.key } )
        tagsByKey.each { key, values ->
            out << '\n#### ' << key.capitalize() << 's:\n\n'
            values.each { tag ->
                out << '* ' << tag.url << '\n'
            }
        }
        if ( feature.attachments.size > 0 ) {
            out << '\n#### ' << 'See:' << '\n\n'
            feature.attachments.each { value ->
                out << '* ' << value.url << '\n'
            } 
        }
    }
    def writePendingFeature = { pendingFeature ->
        if ( pendingFeature ) {
            out << '\n> Pending Feature\n'
        }
    }
    def writeHeaders = { headers ->
        if ( headers ) {
            headers.each { h ->
                out << '> ' << h << '\n'
            }
        }
    }
    def writeExtraInfo = { extraInfo ->
        if ( extraInfo ) {
            extraInfo.each { info ->
                out << '* ' << info << '\n'
            }
        }
    }
    writeHeaders( utils.specHeaders( data ) )
    writeTagOrAttachment data.info
%>

## Features
<%
    features.eachFeature { name, result, blocks, iterations, params ->
%>
### $name
<% 
 writePendingFeature( description.getAnnotation( spock.lang.PendingFeature ) )
 writeTagOrAttachment( delegate )
 if (result != "IGNORED") {
      if ( utils.isUnrolled( delegate ) ) {
          writeExtraInfo( utils.nextSpecExtraInfo( data ) )
      } else {
          // collapse all iterations
          (1..iterations.size()).each {
              writeExtraInfo( utils.nextSpecExtraInfo( data ) )
          }
     }
 }
 def iterationTimes = iterations.collect { it.time ?: 0L }
 def totalTime = fmt.toTimeDuration( iterationTimes.sum() )
%>
Result: **$result**
Time: $totalTime
<%
        for ( block in blocks ) {
 %>
* ${block.kind} ${block.text}
<%
          if ( block.sourceCode ) {
              out << "\n```\n"
              block.sourceCode.each { codeLine ->
                  out << codeLine << '\n'
              }
              out << "```\n"
          }
        }
        def executedIterations = iterations.findAll { it.dataValues || it.errors }
        
        if ( params && executedIterations ) {
            def iterationReportedTimes = executedIterations.collect { it.time ?: 0L }
                        .collect { fmt.toTimeDuration( it ) }
            def maxTimeLength = iterationReportedTimes.collect { it.size() }.sort().last()
 %>
 | ${params.join( ' | ' )} | ${' ' * maxTimeLength} |
 |${params.collect { ( '-' * ( it.size() + 2 ) ) + '|' }.join()}${'-' * ( maxTimeLength + 2 )}|
<%
            executedIterations.eachWithIndex { iteration, i -> 
%> | ${( iteration.dataValues + [ iterationReportedTimes[ i ] ] ).join( ' | ' )} | ${iteration.errors ? '(FAIL)' : '(PASS)'}
<%          }
        }
        def problems = executedIterations.findAll { it.errors }
        if ( problems ) {
            out << "\nThe following problems occurred:\n\n"
            for ( badIteration in problems ) {
                if ( badIteration.dataValues ) {
                    out << '* ' << badIteration.dataValues << '\n'
                }
                for ( error in badIteration.errors ) {
                    out << '```\n' << error << '\n```\n'
                }
            }
        }
    }
 %>