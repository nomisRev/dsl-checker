package org.jetbrains.kotlin.compiler.plugin.template.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtSimpleDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val DSL_ANNOTATION_CLASS_ID = ClassId(
    FqName("org.jetbrains.kotlin.compiler.plugin.template"),
    Name.identifier("DSL"),
)

private object DslCheckerDiagnosticRendererFactory : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("DslCheckerDiagnostics") { map ->
        map.put(DSL_FUNCTION_WITH_PARAMETERS, "DSL functions must not declare value parameters")
    }
}

private val DSL_FUNCTION_WITH_PARAMETERS = KtDiagnosticFactory0(
    "DSL_FUNCTION_WITH_PARAMETERS",
    Severity.ERROR,
    SourceElementPositioningStrategies.DECLARATION_NAME,
    KtSimpleDiagnostic::class,
    DslCheckerDiagnosticRendererFactory,
)

class SimpleAdditionalCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val functionCheckers: Set<FirDeclarationChecker<FirFunction>> = setOf(DslFunctionParameterChecker)
    }
}

private object DslFunctionParameterChecker : FirDeclarationChecker<FirFunction>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (!declaration.hasDslAnnotation() || declaration.valueParameters.isEmpty()) return

        val source = declaration.source ?: return
        reporter.reportOn(
            source,
            DSL_FUNCTION_WITH_PARAMETERS,
            context,
            SourceElementPositioningStrategies.DECLARATION_NAME,
        )
    }

    private fun FirFunction.hasDslAnnotation(): Boolean = annotations.any { it.matchesDslAnnotation() }

    private fun FirAnnotation.matchesDslAnnotation(): Boolean {
        val classLikeType = (annotationTypeRef as? org.jetbrains.kotlin.fir.types.FirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: return false
        return classLikeType.lookupTag.classId == DSL_ANNOTATION_CLASS_ID
    }
}
