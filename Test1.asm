	.section	__TEXT,__text,regular,pure_instructions
	.build_version macos, 10, 14	sdk_version 10, 14
	.globl	_Orb_Core_Main_Main_main_Orb_Core_Main_Main_Orb_Types_Intrinsics_Unit ## -- Begin function Orb_Core_Main_Main_main_Orb_Core_Main_Main_Orb_Types_Intrinsics_Unit
	.p2align	4, 0x90
_Orb_Core_Main_Main_main_Orb_Core_Main_Main_Orb_Types_Intrinsics_Unit: ## @Orb_Core_Main_Main_main_Orb_Core_Main_Main_Orb_Types_Intrinsics_Unit
	.cfi_startproc
## %bb.0:
	pushq	%rbp
	.cfi_def_cfa_offset 16
	.cfi_offset %rbp, -16
	movq	%rsp, %rbp
	.cfi_def_cfa_register %rbp
	leaq	L_.str.2(%rip), %rdi
	popq	%rbp
	jmp	_puts                   ## TAILCALL
	.cfi_endproc
                                        ## -- End function
	.globl	_main                   ## -- Begin function main
	.p2align	4, 0x90
_main:                                  ## @main
	.cfi_startproc
## %bb.0:
	pushq	%rbp
	.cfi_def_cfa_offset 16
	.cfi_offset %rbp, -16
	movq	%rsp, %rbp
	.cfi_def_cfa_register %rbp
	leaq	L_.str.2(%rip), %rdi
	callq	_puts
	xorl	%eax, %eax
	popq	%rbp
	retq
	.cfi_endproc
                                        ## -- End function
	.section	__TEXT,__cstring,cstring_literals
L_.str.2:                               ## @.str.2
	.asciz	"s"


.subsections_via_symbols
