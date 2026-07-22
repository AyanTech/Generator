package com.alirezabdn.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

/**
 * Creates the KSP processor that generates networking layers for declarations annotated with
 * [AyanAPI].
 */
class ProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        Processor(environment.codeGenerator)
}

/**
 * Coordinates symbol discovery, declaration parsing, and source generation.
 *
 * Each generator owns one output concern so this class only manages the KSP processing lifecycle.
 */
private class Processor(
    codeGenerator: CodeGenerator,
) : SymbolProcessor {
    private val declarationParser = ApiDeclarationParser()
    private val functionFactory = ApiFunctionFactory()
    private val remoteDataSourceGenerator =
        RemoteDataSourceGenerator(codeGenerator, functionFactory)
    private val repositoryGenerator = RepositoryGenerator(codeGenerator, functionFactory)
    private val mockGenerator = MockGenerator(codeGenerator, functionFactory)

    /**
     * Generates one remote data source, repository, and matching mocks per separation category.
     *
     * Invalid symbols are deferred to a later KSP round.
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(AyanAPI::class.qualifiedName!!)
            .toList()
        val invalidSymbols = symbols.filterNot(KSAnnotated::validate)
        val apiClasses =
            symbols.filterIsInstance<KSClassDeclaration>().filter(KSAnnotated::validate)

        if (apiClasses.isEmpty() || invalidSymbols.isNotEmpty()) return invalidSymbols

        apiClasses
            .sortedBy { it.qualifiedName?.asString() }
            .map(declarationParser::parse)
            .groupBy { it.data.separationCategory }
            .toSortedMap()
            .forEach { (separationCategory, inputs) ->
                val batch = GenerationBatch(
                    separationCategory = separationCategory,
                    dataNeeds = inputs.map(ProcessorInput::data),
                    sourceFiles = inputs.map(ProcessorInput::sourceFile).distinct(),
                )
                remoteDataSourceGenerator.generate(batch)
                repositoryGenerator.generate(batch)
                mockGenerator.generateRemoteDataSourceMock(batch)
                mockGenerator.generateRepositoryMock(batch)
            }

        return invalidSymbols
    }
}
