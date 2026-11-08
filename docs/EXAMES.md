# Exames no mobile

## Contrato atual

- `GET /api/participantes/{id}/exames`: lista JSON de `ExameDTO`, autenticada pelo JWT do paciente.
- `GET /api/exames/{id}/arquivo`: PDF autenticado, acessado somente quando o paciente solicita visualizar ou baixar.
- O DTO nao fornece titulo clinico do exame. O mobile usa `nomeArquivo`, removendo a extensao e substituindo underscores por espacos para exibicao.
- `dataColeta` e uma data sem fuso. `disponibilizacaoEm` e `publicadoEm` sao instantes UTC e sao exibidos no fuso do aparelho.
- A API atual esconde exames RASCUNHO, INATIVADO e AGENDADO com liberacao futura para a role PACIENTE. Um agendamento vencido aparece como PUBLICADO, mesmo antes do scheduler atualizar a entidade.
- Portanto, o filtro Agendados esta preparado, mas fica vazio enquanto o backend mantiver a regra acima. Nenhuma restricao do servidor e contornada pelo app.

## Implementacao

- O card Meus exames fica na aba Prescricoes e pode ser acessado independentemente do carregamento de medicamentos, alimentacao ou exercicios.
- Tocar no card de um exame abre Detalhes do exame, com status, datas, laboratorio, observacao e acoes de PDF. Voltar retorna a lista preservando o filtro e a posicao de rolagem, sem consultar a API novamente apenas para abrir os detalhes.
- A consulta so acontece ao abrir Meus exames. A listagem tem cache em memoria de dois minutos, atualizacao ao retornar para a tela e atualizacao manual puxando para baixo.
- Exames de outro paciente, IDs invalidos, rascunhos e inativados nao entram na listagem.
- Ver PDF usa o PdfRenderer do Android, renderizando uma pagina por vez com resolucao limitada, navegacao de paginas e zoom.
- Baixar usa o seletor de documentos do Android, sem exigir acesso amplo ao armazenamento. O JWT nao e incluido em URLs publicas ou enviado para leitores externos.
- PDFs sao transferidos por streaming para arquivos temporarios privados, com verificacao de assinatura, final do documento e limite local de 20 MB. A API usa 5 MB por padrao, configuravel no servidor.
- Os arquivos temporarios sao removidos ao fechar a visualizacao, sair da tela, trocar de paciente ou destruir o ViewModel. Falhas de exportacao removem o arquivo incompleto criado pelo seletor quando o provedor permite.
- Uma troca de paciente cancela operacoes e descarta respostas antigas. Os controles bloqueiam operacoes concorrentes de arquivo.

## Testes em aparelho

1. Entrar como PACIENTE com exames publicados e abrir Prescricoes > Meus exames.
2. Conferir nome derivado do PDF, data de coleta, laboratorio, observacao e data de disponibilizacao no fuso local.
3. Alternar Todos, Agendados e Publicados. Na API atual, nao esperar exames agendados para o futuro.
4. Abrir PDF de varias paginas; navegar, ampliar, reduzir e arrastar sem sobrepor o cabecalho ou os controles inferiores.
5. Salvar pelo seletor, abrir o arquivo salvo em um leitor e confirmar que o conteudo esta completo.
6. Cancelar o seletor sem salvar; os botoes devem voltar a ficar disponiveis.
7. Desligar a rede ou expirar a sessao durante o acesso. Conferir mensagem de erro ou retorno ao login, sem fechamento do app.
8. Sair da tela enquanto baixa, girar o aparelho e trocar de paciente. Nenhum PDF ou resposta da conta anterior deve aparecer para a nova conta.
9. Testar PDF protegido ou malformado: a visualizacao deve mostrar erro, mantendo o app aberto.
10. Conferir layouts em tela pequena e com a fonte grande do HDC.

## Verificacao automatizada

`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

Os testes de ExamesTest e PdfTransferTest cobrem status, titularidade, datas/fuso, nomes de arquivo, PDF invalido/incompleto, cancelamento e limite de tamanho. A integracao autenticada, o renderizador nativo e o seletor de arquivos precisam de validacao real em aparelho/emulador.
