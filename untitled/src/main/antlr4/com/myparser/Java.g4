grammar Java;

options {
    superClass = JavaParserBase;
}

compilationUnit
    : packageDeclaration? (importDeclaration | ';')* (typeDeclaration | ';')* EOF
    | modularCompulationUnit EOF
    ;

modularCompulationUnit
    : importDeclaration* moduleDeclaration
    ;

packageDeclaration
    : annotation* PACKAGE qualifiedName ';'
    ;

importDeclaration
    : IMPORT STATIC? qualifiedName ('.' '*')? ';'
    ;

typeDeclaration
    : classOrInterfaceModifier* (
        classDeclaration
        | enumDeclaration
        | interfaceDeclaration
        | annotationTypeDeclaration
        | recordDeclaration
    )
    ;

modifier
    : classOrInterfaceModifier
    | NATIVE
    | SYNCHRONIZED
    | TRANSIENT
    | VOLATILE
    ;

classOrInterfaceModifier
    : annotation
    | PUBLIC
    | PROTECTED
    | PRIVATE
    | STATIC
    | ABSTRACT
    | FINAL // FINAL for class only -- does not apply to interfaces
    | STRICTFP
    | SEALED
    | NON_SEALED
    ;

variableModifier
    : FINAL
    | annotation
    ;

classDeclaration
    : CLASS identifier typeParameters? (EXTENDS typeType)? (IMPLEMENTS typeList)? (
        PERMITS typeList
    )?
    classBody
    ;

typeParameters
    : '<' typeParameter (',' typeParameter)* '>'
    ;

typeParameter
    : annotation* identifier (EXTENDS annotation* typeBound)?
    ;

typeBound
    : typeType ('&' typeType)*
    ;

enumDeclaration
    : ENUM identifier (IMPLEMENTS typeList)? '{' enumConstants? ','? enumBodyDeclarations? '}'
    ;

enumConstants
    : enumConstant (',' enumConstant)*
    ;

enumConstant
    : annotation* identifier arguments? classBody?
    ;

enumBodyDeclarations
    : ';' classBodyDeclaration*
    ;

interfaceDeclaration
    : INTERFACE identifier typeParameters? (EXTENDS typeList)? (PERMITS typeList)? interfaceBody
    ;

classBody
    : '{' classBodyDeclaration* '}'
    ;

interfaceBody
    : '{' interfaceBodyDeclaration* '}'
    ;

classBodyDeclaration
    : ';'
    | STATIC? block
    | modifier* memberDeclaration
    ;

memberDeclaration
    : recordDeclaration
    | methodDeclaration
    | genericMethodDeclaration
    | fieldDeclaration
    | constructorDeclaration
    | genericConstructorDeclaration
    | interfaceDeclaration
    | annotationTypeDeclaration
    | classDeclaration
    | enumDeclaration
    ;

/* We use rule this even for void methods which cannot have [] after parameters.
   This simplifies grammar and we can consider void to be a type, which
   renders the [] matching as a context-sensitive issue or a semantic check
   for invalid return type after parsing.
 */
methodDeclaration
    : typeTypeOrVoid identifier formalParameters ('[' ']')* (THROWS qualifiedNameList)? methodBody
    ;

methodBody
    : block
    | ';'
    ;

typeTypeOrVoid
    : typeType
    | VOID
    ;

genericMethodDeclaration
    : typeParameters methodDeclaration
    ;

genericConstructorDeclaration
    : typeParameters constructorDeclaration
    ;

constructorDeclaration
    : identifier formalParameters (THROWS qualifiedNameList)? constructorBody = block
    ;

compactConstructorDeclaration
    : modifier* identifier constructorBody = block
    ;

fieldDeclaration
    : typeType variableDeclarators ';'
    ;

interfaceBodyDeclaration
    : modifier* interfaceMemberDeclaration
    | ';'
    ;

interfaceMemberDeclaration
    : recordDeclaration
    | constDeclaration
    | interfaceMethodDeclaration
    | genericInterfaceMethodDeclaration
    | interfaceDeclaration
    | annotationTypeDeclaration
    | classDeclaration
    | enumDeclaration
    ;

constDeclaration
    : typeType constantDeclarator (',' constantDeclarator)* ';'
    ;

constantDeclarator
    : identifier ('[' ']')* '=' variableInitializer
    ;

// Early versions of Java allows brackets after the method name, eg.
// public int[] return2DArray() [] { ... }
// is the same as
// public int[][] return2DArray() { ... }
interfaceMethodDeclaration
    : interfaceMethodModifier* interfaceCommonBodyDeclaration
    ;

interfaceMethodModifier
    : annotation
    | PUBLIC
    | ABSTRACT
    | DEFAULT
    | STATIC
    | STRICTFP
    ;

genericInterfaceMethodDeclaration
    : interfaceMethodModifier* typeParameters interfaceCommonBodyDeclaration
    ;

interfaceCommonBodyDeclaration
    : annotation* typeTypeOrVoid identifier formalParameters ('[' ']')* (THROWS qualifiedNameList)? methodBody
    ;

variableDeclarators
    : variableDeclarator (',' variableDeclarator)*
    ;

variableDeclarator
    : variableDeclaratorId ('=' variableInitializer)?
    ;

variableDeclaratorId
    : identifier ('[' ']')*
    ;

variableInitializer
    : arrayInitializer
    | expression
    ;

arrayInitializer
    : '{' (variableInitializer (',' variableInitializer)* ','?)? '}'
    ;

classType:
    (
      ( packageName '.' annotation* )? typeIdentifier typeArguments?
    )+ ( '.' annotation* typeIdentifier typeArguments? )*
    ;

packageName:
    identifier ('.' identifier)*
    ;

typeArgument
    : typeType
    | annotation* '?' ((EXTENDS | SUPER) typeType)?
    ;

qualifiedNameList
    : qualifiedName (',' qualifiedName)*
    ;

formalParameters
    : '(' (
       ( receiverParameter | formalParameter ) (',' formalParameterList)*
    )? ')'
    ;

receiverParameter
    : typeType (identifier '.')* THIS
    ;

formalParameterList
    : formalParameter (',' formalParameter)*
    ;

formalParameter
    : variableModifier* typeType (annotation* '...')? variableDeclaratorId
    ;

// local variable type inference
lambdaLVTIList
    : lambdaLVTIParameter (',' lambdaLVTIParameter)*
    ;

lambdaLVTIParameter
    : variableModifier* VAR identifier
    ;

qualifiedName
    : identifier ('.' identifier)*
    ;

literal
    : integerLiteral
    | floatLiteral
    | CHAR_LITERAL
    | STRING_LITERAL
    | BOOL_LITERAL
    | NULL_LITERAL
    | TEXT_BLOCK
    ;

integerLiteral
    : DECIMAL_LITERAL
    | HEX_LITERAL
    | OCT_LITERAL
    | BINARY_LITERAL
    ;

floatLiteral
    : FLOAT_LITERAL
    | HEX_FLOAT_LITERAL
    ;

// ANNOTATIONS
altAnnotationQualifiedName
    : (identifier DOT)* '@' identifier
    ;

//annotation
//    : ('@' qualifiedName /* | altAnnotationQualifiedName */) ( '(' ( elementValuePairs | elementValue)? ')')?
//    ;

annotation :
    ('@' qualifiedName /* | altAnnotationQualifiedName */) annotationFieldValues?
    ;

annotationFieldValues:
	'(' ( annotationFieldValue ( ',' annotationFieldValue )* )? ')'
	;

annotationFieldValue:
	{ this.IsNotIdentifierAssign() }? annotationValue
	| identifier '=' annotationValue
	;

annotationValue:
	expression //conditionalExpression
	| annotation
	| '{' ( annotationValue ( ',' annotationValue )* )? ','? '}'
	;

//elementValuePairs
//    : elementValuePair (',' elementValuePair)*
//    ;

//elementValuePair
//    : identifier '=' elementValue
//    ;

elementValue
    : expression
    | annotation
    | elementValueArrayInitializer
    ;

elementValueArrayInitializer
    : '{' (elementValue (',' elementValue)*)? ','? '}'
    ;

annotationTypeDeclaration
    : '@' INTERFACE identifier annotationTypeBody
    ;

annotationTypeBody
    : '{' annotationTypeElementDeclaration* '}'
    ;

annotationTypeElementDeclaration
    : modifier* annotationTypeElementRest
    | ';' // this is not allowed by the grammar, but apparently allowed by the actual compiler
    ;

annotationTypeElementRest
    : typeType annotationMethodOrConstantRest ';'
    | classDeclaration ';'?
    | interfaceDeclaration ';'?
    | enumDeclaration ';'?
    | annotationTypeDeclaration ';'?
    | recordDeclaration ';'?
    ;

annotationMethodOrConstantRest
    : annotationMethodRest
    | annotationConstantRest
    ;

annotationMethodRest
    : identifier '(' ')' defaultValue?
    ;

annotationConstantRest
    : variableDeclarators
    ;

defaultValue
    : DEFAULT elementValue
    ;

moduleDeclaration
    : annotation* OPEN? MODULE qualifiedName '{' moduleDirective* '}'
    ;

moduleDirective
    : REQUIRES requiresModifier* qualifiedName ';'
    | EXPORTS qualifiedName (TO qualifiedName (',' qualifiedName)* )? ';'
    | OPENS qualifiedName (TO qualifiedName (',' qualifiedName)* )? ';'
    | USES qualifiedName ';'
    | PROVIDES qualifiedName WITH qualifiedName (',' qualifiedName)* ';'
    ;

requiresModifier
    : TRANSITIVE
    | STATIC
    ;

recordDeclaration
    : RECORD identifier typeParameters? recordHeader (IMPLEMENTS typeList)? recordBody
    ;

recordHeader
    : '(' recordComponentList? ')'
    ;

recordComponentList
    : recordComponent (',' recordComponent)* { this.DoLastRecordComponent() }?
    ;

recordComponent
    : annotation* typeType (annotation* ELLIPSIS)? identifier
    ;

recordBody
    : '{' (classBodyDeclaration | compactConstructorDeclaration)* '}'
    ;

// STATEMENTS / BLOCKS

block
    : '{' blockStatement* '}'
    ;

blockStatement
    : localVariableDeclaration ';'
    | localTypeDeclaration
    | statement
    ;

localVariableDeclaration
    : variableModifier* (VAR identifier '=' expression | typeType variableDeclarators)
    ;

identifier
    : IDENTIFIER
    | MODULE
    | OPEN
    | REQUIRES
    | EXPORTS
    | OPENS
    | TO
    | USES
    | PROVIDES
    | WHEN
    | WITH
    | TRANSITIVE
    | YIELD
    | SEALED
    | PERMITS
    | RECORD
    | VAR
    ;

typeIdentifier // Identifiers that are not restricted for type declarations
    : IDENTIFIER
    | MODULE
    | OPEN
    | REQUIRES
    | EXPORTS
    | OPENS
    | TO
    | USES
    | PROVIDES
    | WITH
    | TRANSITIVE
    | SEALED
    ;

localTypeDeclaration
    : classOrInterfaceModifier* (classDeclaration | interfaceDeclaration | recordDeclaration | enumDeclaration)
    ;

statement
    : blockLabel = block
    | ASSERT expression (':' expression)? ';'
    | IF '(' expression ')' statement (ELSE statement)?
    | FOR '(' forControl ')' statement
    | WHILE '(' expression ')' statement
    | DO statement WHILE '(' expression ')' ';'
    | TRY block (catchClause+ finallyBlock? | finallyBlock)
    | TRY resourceSpecification block catchClause* finallyBlock?
    | SWITCH '(' expression ')' '{' switchBlockStatementGroup* switchLabel* '}'
    | SYNCHRONIZED '(' expression ')' block
    | RETURN expression? ';'
    | THROW expression ';'
    | BREAK identifier? ';'
    | CONTINUE identifier? ';'
    | YIELD expression ';'
    | SEMI
    | statementExpression = expression ';'
    | switchExpression ';'?
    | identifierLabel = identifier ':' statement
    ;

catchClause
    : CATCH '(' variableModifier* catchType identifier ')' block
    ;

catchType
    : qualifiedName ('|' qualifiedName)*
    ;

finallyBlock
    : FINALLY block
    ;

resourceSpecification
    : '(' resources ';'? ')'
    ;

resources
    : resource (';' resource)*
    ;

resource
    : variableModifier* (classOrInterfaceType variableDeclaratorId | VAR identifier) '=' expression
    | qualifiedName
    ;

/** Matches cases then statements, both of which are mandatory.
 *  To handle empty cases at the end, we add switchLabel* to statement.
 */
switchBlockStatementGroup
    : (switchLabel ':')+ blockStatement+
    ;

switchLabel
    : CASE (
        constantExpression = expression
        | enumConstantName = IDENTIFIER
        | typeType varName = identifier
    )
    | DEFAULT
    ;

forControl
    : enhancedForControl
    | forInit? ';' expression? ';' forUpdate = expressionList?
    ;

forInit
    : localVariableDeclaration
    | expressionList
    ;

enhancedForControl
    : variableModifier* (typeType | VAR) variableDeclaratorId ':' expression
    ;

// EXPRESSIONS

expressionList
    : expression (',' expression)*
    ;

methodCall
    : (identifier | THIS | SUPER) arguments
    ;

expression
    // Expression order in accordance with https://introcs.cs.princeton.edu/java/11precedence/
    // Level 16, Primary, array and member access
    : primary                                                       #PrimaryExpression
    | expression '[' expression ']'                                 #SquareBracketExpression
    | expression bop = '.' (
        identifier
        | methodCall
        | THIS
        | NEW nonWildcardTypeArguments? innerCreator
        | SUPER superSuffix
        | explicitGenericInvocation
    )                                                               #MemberReferenceExpression
    // Method calls and method references are part of primary, and hence level 16 precedence
    | methodCall                                                    #MethodCallExpression
    | expression '::' typeArguments? identifier                     #MethodReferenceExpression
    | typeType '::' (typeArguments? identifier | NEW)               #MethodReferenceExpression
    | classType '::' typeArguments? NEW                             #MethodReferenceExpression
    
    | switchExpression                                              #ExpressionSwitch

    // Level 15 Post-increment/decrement operators
    | expression postfix = ('++' | '--')                            #PostIncrementDecrementOperatorExpression

    // Level 14, Unary operators
    | prefix = ('+' | '-' | '++' | '--' | '~' | '!') expression     #UnaryOperatorExpression

    // Level 13 Cast and object creation
    | '(' annotation* typeType ('&' typeType)* ')' expression       #CastExpression
    | NEW creator                                                   #ObjectCreationExpression

    // Level 12 to 1, Remaining operators
    // Level 12, Multiplicative operators
    | expression bop = ('*' | '/' | '%') expression           #BinaryOperatorExpression
    // Level 11, Additive operators
    | expression bop = ('+' | '-') expression                 #BinaryOperatorExpression
    // Level 10, Shift operators
    | expression ('<' '<' | '>' '>' '>' | '>' '>') expression #BinaryOperatorExpression
    // Level 9, Relational operators
    | expression bop = ('<=' | '>=' | '>' | '<') expression   #BinaryOperatorExpression
    | expression bop = INSTANCEOF (typeType | pattern)        #InstanceOfOperatorExpression
    // Level 8, Equality Operators
    | expression bop = ('==' | '!=') expression               #BinaryOperatorExpression
    // Level 7, Bitwise AND
    | expression bop = '&' expression                         #BinaryOperatorExpression
    // Level 6, Bitwise XOR
    | expression bop = '^' expression                         #BinaryOperatorExpression
    // Level 5, Bitwise OR
    | expression bop = '|' expression                         #BinaryOperatorExpression
    // Level 4, Logic AND
    | expression bop = '&&' expression                        #BinaryOperatorExpression
    // Level 3, Logic OR
    | expression bop = '||' expression                        #BinaryOperatorExpression
    // Level 2, Ternary
    | <assoc = right> expression bop = '?' expression ':' expression #TernaryExpression
    // Level 1, Assignment
    | <assoc = right> expression bop = (
        '='
        | '+='
        | '-='
        | '*='
        | '/='
        | '&='
        | '|='
        | '^='
        | '>>='
        | '>>>='
        | '<<='
        | '%='
    ) expression                                              #BinaryOperatorExpression

    // Level 0, Lambda Expression
    | lambdaExpression                                        #ExpressionLambda
    ;

pattern
    : variableModifier* typeType annotation* variableDeclarators
    | typeType '(' componentPatternList? ')'
    ;

componentPatternList :
    componentPattern ( ',' componentPattern )*
    ;

componentPattern :
    pattern
    ;

lambdaExpression
    : lambdaParameters '->' lambdaBody
    ;

lambdaParameters
    : identifier
    | '(' formalParameterList? ')'
    | '(' identifier (',' identifier)* ')'
    | '(' lambdaLVTIList? ')'
    ;

lambdaBody
    : expression
    | block
    ;

primary
    : '(' expression ')'
    | THIS
    | SUPER
    | literal
    | identifier
    | typeTypeOrVoid '.' CLASS
    | nonWildcardTypeArguments (explicitGenericInvocationSuffix | THIS arguments)
    ;

switchExpression
    : SWITCH '(' expression ')' '{' switchLabeledRule* '}'
    ;

switchLabeledRule
    : CASE (
	expressionList
	| NULL_LITERAL (',' DEFAULT)?
	| casePattern (',' casePattern)* guard?
	) (ARROW | COLON) switchRuleOutcome
    | DEFAULT (ARROW | COLON) switchRuleOutcome
    ;

guard 
    : 'when' expression
    ;

casePattern
    : pattern
    ;

switchRuleOutcome
    : block
    | blockStatement* // is *-operator correct??? I don't think so. https://docs.oracle.com/javase/specs/jls/se24/html/jls-14.html#jls-BlockStatements
    ;

classOrInterfaceType
    : classType // classType, interfaceType are all essentially identical to classOrInterfaceType because of no symbol table.
    ;

creator
    : nonWildcardTypeArguments? createdName classCreatorRest
    | createdName arrayCreatorRest
    ;

createdName
    : identifier typeArgumentsOrDiamond? ('.' identifier typeArgumentsOrDiamond?)*
    | primitiveType
    ;

innerCreator
    : identifier nonWildcardTypeArgumentsOrDiamond? classCreatorRest
    ;

arrayCreatorRest
    : ('[' ']')+ arrayInitializer
    | ('[' expression ']')+ ('[' ']')*
    ;

classCreatorRest
    : arguments classBody?
    ;

explicitGenericInvocation
    : nonWildcardTypeArguments explicitGenericInvocationSuffix
    ;

typeArgumentsOrDiamond
    : '<' '>'
    | typeArguments
    ;

nonWildcardTypeArgumentsOrDiamond
    : '<' '>'
    | nonWildcardTypeArguments
    ;

nonWildcardTypeArguments
    : '<' typeList '>'
    ;

typeList
    : typeType (',' typeType)*
    ;

typeType
    : annotation* (classOrInterfaceType | primitiveType) (annotation* '[' ']')*
    ;

primitiveType
    : BOOLEAN
    | CHAR
    | BYTE
    | SHORT
    | INT
    | LONG
    | FLOAT
    | DOUBLE
    ;

typeArguments
    : '<' typeArgument (',' typeArgument)* '>'
    ;

superSuffix
    : arguments
    | '.' typeArguments? identifier arguments?
    ;

explicitGenericInvocationSuffix
    : SUPER superSuffix
    | identifier arguments
    ;

arguments
    : '(' expressionList? ')'
    ;
ABSTRACT     : 'abstract';
ASSERT       : 'assert';
BOOLEAN      : 'boolean';
BREAK        : 'break';
BYTE         : 'byte';
CASE         : 'case';
CATCH        : 'catch';
CHAR         : 'char';
CLASS        : 'class';
CONST        : 'const';
CONTINUE     : 'continue';
DEFAULT      : 'default';
DO           : 'do';
DOUBLE       : 'double';
ELSE         : 'else';
ENUM         : 'enum';
EXPORTS    : 'exports';
EXTENDS      : 'extends';
FINAL        : 'final';
FINALLY      : 'finally';
FLOAT        : 'float';
FOR          : 'for';
GOTO         : 'goto';
IF           : 'if';
IMPLEMENTS   : 'implements';
IMPORT       : 'import';
INSTANCEOF   : 'instanceof';
INT          : 'int';
INTERFACE    : 'interface';
LONG         : 'long';
MODULE     : 'module';
NATIVE       : 'native';
NEW          : 'new';
NON_SEALED : 'non-sealed';
OPEN       : 'open';
OPENS      : 'opens';
PACKAGE      : 'package';
PERMITS    : 'permits';
PRIVATE      : 'private';
PROTECTED    : 'protected';
PROVIDES   : 'provides';
PUBLIC       : 'public';
RECORD: 'record';
REQUIRES   : 'requires';
RETURN       : 'return';
SEALED     : 'sealed';
SHORT        : 'short';
STATIC       : 'static';
STRICTFP     : 'strictfp';
SUPER        : 'super';
SWITCH       : 'switch';
SYNCHRONIZED : 'synchronized';
THIS         : 'this';
THROW        : 'throw';
THROWS       : 'throws';
TO         : 'to';
TRANSIENT    : 'transient';
TRANSITIVE : 'transitive';
TRY          : 'try';
USES       : 'uses';
VAR: 'var'; // reserved type name
VOID         : 'void';
VOLATILE     : 'volatile';
WHEN : 'when';
WHILE        : 'while';
WITH       : 'with';
YIELD: 'yield'; // reserved type name from Java 14

// Literals

DECIMAL_LITERAL : ('0' | [1-9] (Digits? | '_'+ Digits)) [lL]?;
HEX_LITERAL     : '0' [xX] [0-9a-fA-F] ([0-9a-fA-F_]* [0-9a-fA-F])? [lL]?;
OCT_LITERAL     : '0' '_'* [0-7] ([0-7_]* [0-7])? [lL]?;
BINARY_LITERAL  : '0' [bB] [01] ([01_]* [01])? [lL]?;

FLOAT_LITERAL:
    (Digits '.' Digits? | '.' Digits) ExponentPart? [fFdD]?
    | Digits (ExponentPart [fFdD]? | [fFdD])
;

HEX_FLOAT_LITERAL: '0' [xX] (HexDigits '.'? | HexDigits? '.' HexDigits) [pP] [+-]? Digits [fFdD]?;

BOOL_LITERAL: 'true' | 'false';

CHAR_LITERAL: '\'' (~['\\\r\n] | EscapeSequence) '\'';

STRING_LITERAL: '"' (~["\\\r\n] | EscapeSequence)* '"';

TEXT_BLOCK: '"""' [ \t]* [\r\n] (. | EscapeSequence)*? '"""';

NULL_LITERAL: 'null';

// Separators

LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
RBRACE : '}';
LBRACK : '[';
RBRACK : ']';
SEMI   : ';';
COMMA  : ',';
DOT    : '.';

// Operators

ASSIGN   : '=';
GT       : '>';
LT       : '<';
BANG     : '!';
TILDE    : '~';
QUESTION : '?';
COLON    : ':';
EQUAL    : '==';
LE       : '<=';
GE       : '>=';
NOTEQUAL : '!=';
AND      : '&&';
OR       : '||';
INC      : '++';
DEC      : '--';
ADD      : '+';
SUB      : '-';
MUL      : '*';
DIV      : '/';
BITAND   : '&';
BITOR    : '|';
CARET    : '^';
MOD      : '%';

ADD_ASSIGN     : '+=';
SUB_ASSIGN     : '-=';
MUL_ASSIGN     : '*=';
DIV_ASSIGN     : '/=';
AND_ASSIGN     : '&=';
OR_ASSIGN      : '|=';
XOR_ASSIGN     : '^=';
MOD_ASSIGN     : '%=';
LSHIFT_ASSIGN  : '<<=';
RSHIFT_ASSIGN  : '>>=';
URSHIFT_ASSIGN : '>>>=';

// Java 8 tokens

ARROW      : '->';
COLONCOLON : '::';

// Additional symbols not defined in the lexical specification

AT       : '@';
ELLIPSIS : '...';

// Whitespace and comments

WS           : [ \t\r\n\u000C]+ -> channel(HIDDEN);
COMMENT      : '/*' .*? '*/'    -> channel(HIDDEN);
LINE_COMMENT : '//' ~[\r\n]*    -> channel(HIDDEN);

// Identifiers

IDENTIFIER: Letter LetterOrDigit*;

// Fragment rules

fragment ExponentPart: [eE] [+-]? Digits;

fragment EscapeSequence:
    '\\' 'u005c'? [bstnfr"'\\]
    | '\\' 'u005c'? ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
;

fragment HexDigits: HexDigit ((HexDigit | '_')* HexDigit)?;

fragment HexDigit: [0-9a-fA-F];

fragment Digits: [0-9] ([0-9_]* [0-9])?;

fragment LetterOrDigit: Letter | [0-9];

fragment Letter:
    [a-zA-Z$_]                        // these are the "java letters" below 0x7F
    | ~[\u0000-\u007F\uD800-\uDBFF]   // covers all characters above 0x7F which are not a surrogate
    | [\uD800-\uDBFF] [\uDC00-\uDFFF] // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
;