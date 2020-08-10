package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.util.Utils
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.apache.commons.lang3.StringUtils
import spock.lang.Title

import static com.athaydes.spockframework.report.internal.ReportDataAggregator.getAllAggregatedDataAndPersistLocalData
import static com.athaydes.spockframework.report.internal.SpecSummaryNameOption.CLASS_NAME
import static com.athaydes.spockframework.report.internal.SpecSummaryNameOption.CLASS_NAME_AND_TITLE
import static com.athaydes.spockframework.report.internal.SpecSummaryNameOption.TITLE

/**
 *
 * User: Renato
 */
@Slf4j
class HtmlReportAggregator extends AbstractHtmlCreator<Map> {

    final Map<String, Map> aggregatedData = [ : ]
    final List <Map> reportJsonDataList = []

    def stringFormatter = new StringFormatHelper()

    String projectName
    String projectVersion
    String aggregatedJsonReportDir
    SpecSummaryNameOption specSummaryNameOption = CLASS_NAME_AND_TITLE

    protected HtmlReportAggregator() {
        // provided for testing only (need to Mock it)
    }

    @Override
    String cssDefaultName() { 'summary-report.css' }

    void aggregateReport( SpecData data, Map stats ) {
        def specName = Utils.getSpecClassName( data )
        def allFeatures = data.info.allFeaturesInExecutionOrder.groupBy { feature -> Utils.isSkipped( feature ) }

        def specTitle = Utils.specAnnotation( data, Title )?.value() ?: ''
        def narrative = data.info.narrative ?: ''

        aggregatedData[ specName ] = Utils.createAggregatedData(
                allFeatures[ false ], allFeatures[ true ], stats, specTitle, narrative )
    }

    void jsonListReport(SpecData data, Map stats){
        def specName = Utils.getSpecClassName( data )
        def allFeatures = data.info.allFeaturesInExecutionOrder.groupBy { feature -> Utils.isSkipped( feature ) }

        def specTitle = Utils.specAnnotation( data, Title )?.value() ?: ''
        def narrative = data.info.narrative ?: ''

        reportJsonDataList.add( Utils.createReportListData( specName,specTitle,narrative,data.totalTime,stats.failures,
                allFeatures[ false ]))
    }

    void writeOut() {
        final reportsDir = outputDirectory as File // try to force it into being a File!
        if ( existsOrCanCreate( reportsDir ) ) {
            final aggregatedReport = new File( reportsDir, 'index.html' )
            final reportJson = new File(reportsDir,"report.json")
            final jsonDir = aggregatedJsonReportDir ? new File( aggregatedJsonReportDir ) : reportsDir

            try {
                def allData = getAllAggregatedDataAndPersistLocalData( jsonDir, aggregatedData )
                aggregatedData.clear()

                //Generate the reportJson file
                //getAllAggregatedDataAndPersistLocalData( jsonDir,"report.json", reportJsonDataList )
                //reportJsonDataList.clear()
                aggregatedReport.write( reportFor( allData ), 'UTF-8' )
            } catch ( e ) {
                log.warn( "Failed to create aggregated report", e )
            }
        } else {
            log.warn "Cannot create output directory: {}", reportsDir?.absolutePath
        }
    }

    static boolean existsOrCanCreate( File reportsDir ) {
        reportsDir?.exists() || reportsDir?.mkdirs()
    }

    @Override
    protected String reportHeader( Map data ) {
        '测试结果'
    }

    @Override
    protected void writeSummary( MarkupBuilder builder, Map json ) {
        def stats = Utils.aggregateStats( json )
        def cssClassIfTrue = { isTrue, String cssClass ->
            if ( isTrue ) [ 'class': cssClass ] else Collections.emptyMap()
        }

        if ( projectName && projectVersion ) {
            builder.div( 'class': 'project-header' ) {
                span( 'class': 'project-name', "Project: ${projectName}" )
                span( 'class': 'project-version', "Version: ${projectVersion}" )
            }
        }

        builder.div( 'class': 'summary-report' ) {
            h3 '测试统计:'
            //builder.div( 'class': 'date-test-ran', whenAndWho.whenAndWhoRanTest( stringFormatter ) )
            table( 'class': 'summary-table' ) {
                thead {
                    th '总计'
                    th '成功'
                    th '失败'
                    th '用例错误数目'
                    th '成功比率'
                    th '时间统计'
                }
                tbody {
                    tr {
                        td stats.total
                        td stats.passed
                        td( cssClassIfTrue( stats.failed, 'failure' ), stats.failed )
                        td( cssClassIfTrue( stats.fErrors, 'error' ), stats.fErrors )
                        td( cssClassIfTrue( stats.failed, 'failure' ), stringFormatter
                                .toPercentage( Utils.successRate( stats.total, stats.failed ) ) )
                        td stringFormatter.toTimeDuration( stats.time )
                    }
                }
            }
        }
    }

    @Override
    protected void writeDetails( MarkupBuilder builder, Map data ) {
        builder.h3 '测试概况:'
        builder.table( 'class': 'summary-table' ) {
            thead {
                th '测试集'
                th '测试用例'
                th '失败'
                th '错误'
                th '跳过'
                th '成功比率'
                th '描述'
                th '时间'
            }
            tbody {
                data.keySet().each { String specName ->
                    def stats = data[ specName ].stats
                    def title = data[ specName ].title
                    def features = data[specName].executedFeatures
                    writeSpecSummary( builder, stats, specName,features, title )
                }
            }
        }

    }
    //data.keySet().sort().each { String specName ->


    protected void writeSpecSummary( MarkupBuilder builder, Map stats, String specName, List features,String title ) {
        def cssClasses = [ ]
        if ( stats.totalRuns == 0 ) {
            cssClasses << 'ignored'
        } else {
            if ( stats.failures ) cssClasses << 'failure'
            if ( stats.errors ) cssClasses << 'error'
        }
        builder.tr( cssClasses ? [ 'class': cssClasses.join( ' ' ) ] : null ) {
            td {
                switch ( specSummaryNameOption ) {
                    case CLASS_NAME_AND_TITLE:
                        a( href: "${specName}.html", specName )
                        if ( title ) {
                            div( 'class': 'spec-title', title )
                        }
                        break
                    case CLASS_NAME:
                        a( href: "${specName}.html", specName )
                        break
                    case TITLE:
                        if ( title ) {
                            a( href: "${specName}.html" ) {
                                div( 'class': 'spec-title', title )
                            }
                        } else {
                            a( href: "${specName}.html", specName )
                        }
                        break
                }
            }
            td stats.totalFeatures
            td stats.failures
            td stats.errors
            td stats.skipped
            td stringFormatter.toPercentage( stats.successRate )
            td {
                features.each {
                    builder.p it
                }
            }
            td stringFormatter.toTimeDuration( stats.time )
        }
    }

    protected void writeSpecSummary( MarkupBuilder builder, Map stats, String specName, String title ) {
        def cssClasses = [ ]
        if ( stats.totalRuns == 0 ) {
            cssClasses << 'ignored'
        } else {
            if ( stats.failures ) cssClasses << 'failure'
            if ( stats.errors ) cssClasses << 'error'
        }
        builder.tr( cssClasses ? [ 'class': cssClasses.join( ' ' ) ] : null ) {
            td {
                switch ( specSummaryNameOption ) {
                    case CLASS_NAME_AND_TITLE:
                        a( href: "${specName}.html", specName )
                        if ( title ) {
                            div( 'class': 'spec-title', title )
                        }
                        break
                    case CLASS_NAME:
                        a( href: "${specName}.html", specName )
                        break
                    case TITLE:
                        if ( title ) {
                            a( href: "${specName}.html" ) {
                                div( 'class': 'spec-title', title )
                            }
                        } else {
                            a( href: "${specName}.html", specName )
                        }
                        break
                }
            }
            td stats.totalFeatures
            td stats.failures
            td stats.errors
            td stats.skipped
            td stringFormatter.toPercentage( stats.successRate )
            td stringFormatter.toTimeDuration( stats.time )
        }
    }
}
