@file:OptIn(org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess::class, org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)

package org.jetbrains.kotlin.compiler.plugin.template.fir

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtSimpleDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.isExhaustive
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.expressions.unexpandedConeClassLikeType
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.receiverType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val DSL_ANNOTATION_CLASS_ID = ClassId(
    FqName("org.jetbrains.kotlin.compiler.plugin.template"),
    Name.identifier("DSL"),
)

private val REQUIRED_ANNOTATION_CLASS_ID = ClassId(
    FqName("org.jetbrains.kotlin.compiler.plugin.template"),
    Name.identifier("Required"),
)

private object DslCheckerDiagnosticRendererFactory : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("DslCheckerDiagnostics") { map ->
        map.put(DSL_CALL_MISSING_REQUIRED_PROPERTIES, "{0}", Renderer { it })
    }
}

private val DSL_CALL_MISSING_REQUIRED_PROPERTIES = KtDiagnosticFactory1<String>(
    "DSL_CALL_MISSING_REQUIRED_PROPERTIES",
    Severity.ERROR,
    SourceElementPositioningStrategies.WHOLE_ELEMENT,
    KtSimpleDiagnostic::class,
    DslCheckerDiagnosticRendererFactory,
)

class SimpleAdditionalCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers = setOf(DslFunctionCallChecker)
    }
}

private object DslFunctionCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val calleeSymbol = expression.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
        if (!calleeSymbol.isDslAnnotated()) return

        val lambdaArgument = expression.argumentList.arguments.filterIsInstance<FirAnonymousFunctionExpression>().singleOrNull() ?: return
        val receiverType = calleeSymbol.dslReceiverType(context.session) ?: return
        val requiredProperties = receiverType.requiredDslProperties(context.session)
        if (requiredProperties.isEmpty()) return

        val assignedProperties = lambdaArgument.anonymousFunction.body.definitelyAssignedDslProperties(requiredProperties.map { it.symbol }.toSet())
        val missingProperties = requiredProperties.filterNot { it.symbol in assignedProperties }
        if (missingProperties.isEmpty()) return

        val diagnosticMessage = if (missingProperties.size == 1) {
            missingProperties.single().message
        } else {
            "DSL lambda is missing required properties: ${missingProperties.joinToString(", ") { it.message }}"
        }

        reporter.reportOn(
            expression.source,
            DSL_CALL_MISSING_REQUIRED_PROPERTIES,
            diagnosticMessage,
            context,
            SourceElementPositioningStrategies.WHOLE_ELEMENT,
        )
    }

    private fun FirNamedFunctionSymbol.isDslAnnotated(): Boolean =
        resolvedAnnotationClassIds.any { it == DSL_ANNOTATION_CLASS_ID }

    private fun FirNamedFunctionSymbol.dslReceiverType(session: FirSession): ConeKotlinType? =
        fir.valueParameters.singleOrNull()?.returnTypeRef?.coneType?.receiverType(session)

    private data class RequiredDslProperty(
        val symbol: FirPropertySymbol,
        val message: String,
    )

    private fun ConeKotlinType.requiredDslProperties(session: FirSession): List<RequiredDslProperty> {
        val classId = (this as? ConeClassLikeType)?.lookupTag?.classId ?: return emptyList()
        val classSymbol = session.getRegularClassSymbolByClassId(classId) ?: return emptyList()
        return classSymbol.declarationSymbols
            .filterIsInstance<FirPropertySymbol>()
            .mapNotNull { property ->
                val requiredAnnotation = property.resolvedAnnotationsWithArguments
                    .firstOrNull { annotation -> annotation.isRequiredAnnotation() }
                    ?: return@mapNotNull null
                RequiredDslProperty(
                    symbol = property,
                    message = requiredAnnotation.requiredMessage(property),
                )
            }
    }

    private fun FirAnnotation.isRequiredAnnotation(): Boolean =
        unexpandedConeClassLikeType?.lookupTag?.classId == REQUIRED_ANNOTATION_CLASS_ID

    private fun FirAnnotation.requiredMessage(property: FirPropertySymbol): String =
        annotationArgument("message")?.takeIf { it.isNotBlank() } ?: property.name.asString()

    private fun FirAnnotation.annotationArgument(name: String): String? =
        argumentMapping.mapping[Name.identifier(name)]
            ?.let { it as? FirLiteralExpression }
            ?.value as? String

    private fun FirBlock?.definitelyAssignedDslProperties(
        requiredProperties: Set<FirPropertySymbol>,
        assignedBefore: Set<FirPropertySymbol> = emptySet(),
    ): Set<FirPropertySymbol> {
        if (this == null) return assignedBefore

        var assigned = assignedBefore
        for (statement in statements) {
            assigned = statement.definitelyAssignedDslPropertiesAfter(requiredProperties, assigned)
        }
        return assigned
    }

    private fun FirStatement.definitelyAssignedDslPropertiesAfter(
        requiredProperties: Set<FirPropertySymbol>,
        assignedBefore: Set<FirPropertySymbol>,
    ): Set<FirPropertySymbol> = when (this) {
        is FirVariableAssignment -> {
            val assignedProperty = (lValue as? FirQualifiedAccessExpression)
                ?.toResolvedCallableSymbol() as? FirPropertySymbol
            if (assignedProperty != null && assignedProperty in requiredProperties) assignedBefore + assignedProperty else assignedBefore
        }

        is FirExpression -> this.definitelyAssignedDslPropertiesAfter(requiredProperties, assignedBefore)

        else -> assignedBefore
    }

    private fun FirExpression.definitelyAssignedDslPropertiesAfter(
        requiredProperties: Set<FirPropertySymbol>,
        assignedBefore: Set<FirPropertySymbol>,
    ): Set<FirPropertySymbol> = when (this) {
        is FirBlock -> this.definitelyAssignedDslProperties(requiredProperties, assignedBefore)

        is FirWhenExpression -> this.definitelyAssignedDslPropertiesAfter(requiredProperties, assignedBefore)

        else -> assignedBefore
    }

    private fun FirWhenExpression.definitelyAssignedDslPropertiesAfter(
        requiredProperties: Set<FirPropertySymbol>,
        assignedBefore: Set<FirPropertySymbol>,
    ): Set<FirPropertySymbol> {
        if (!isExhaustive || branches.isEmpty()) return assignedBefore

        var assignedInAllBranches: Set<FirPropertySymbol>? = null
        for (branch in branches) {
            val branchAssigned = branch.result.definitelyAssignedDslProperties(requiredProperties, assignedBefore)
            assignedInAllBranches = assignedInAllBranches?.intersect(branchAssigned)?.toSet() ?: branchAssigned
        }
        return assignedInAllBranches ?: assignedBefore
    }
}
