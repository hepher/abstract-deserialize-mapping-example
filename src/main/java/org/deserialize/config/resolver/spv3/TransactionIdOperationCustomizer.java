package com.enelx.bfw.framework.resolver;

import org.springframework.stereotype.Component;

@Component
public class TransactionIdOperationCustomizer extends AbstractOperationCustomizer<TransactionId> {

    public TransactionIdOperationCustomizer() {
        super(TransactionId.class, (annotation) -> new OperationDetail(annotation.value(), annotation.required(), annotation.type(), annotation.description(), null));
    }

//    @Override
//    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
//
//        String parameterName = null;
//        TransactionId transactionIdAnnotation = null;
//        for (MethodParameter methodParameter : Arrays.stream(handlerMethod.getMethodParameters()).toList()) {
//            if (methodParameter.hasParameterAnnotation(TransactionId.class)) {
//                parameterName = methodParameter.getParameter().getName();
//                transactionIdAnnotation = methodParameter.getParameter().getAnnotation(TransactionId.class);
//            }
//        }
//
//        if (parameterName != null) {
//            for (Parameter parameter : operation.getParameters()) {
//                if (parameterName.equals(parameter.getName())) {
//                    parameter.setName(transactionIdAnnotation.value());
//                    parameter.setIn(transactionIdAnnotation.type().getValue());
//                    parameter.setRequired(transactionIdAnnotation.required());
//                    parameter.setExample(transactionIdAnnotation.description());
//                }
//            }
//        }
//
//        return operation;
//    }
}
