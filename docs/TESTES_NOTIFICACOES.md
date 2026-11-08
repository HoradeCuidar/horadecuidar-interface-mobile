# Testes de lembretes locais

Somente o mobile foi alterado. Os lembretes nao criam nem confirmam registros de adesao no backend.

## Referencia de horario

- Cada item de medicamento possui sua propria referencia, identificada pelo paciente, prescricao e item.
- O primeiro registro REALIZADO confirmado pela API salva o horario local do clique. NAO_REALIZADO e falhas de envio nao iniciam a contagem.
- Registros seguintes nao deslocam essa referencia. Desfazer o registro inicial remove a referencia e sua agenda; um novo registro REALIZADO inicia outra referencia.
- A atualizacao exige um primeiro registro confirmado neste dispositivo para iniciar cada agenda. Registros antigos nao recebem horarios presumidos.
- HORA preserva o intervalo ao atravessar a meia-noite. DIA e SEMANA usam o calendario local. MES conserva o dia do registro inicial e ignora meses sem esse dia, como o backend.
- O lembrete pede para conferir a prescricao: nao calcula novas doses nem substitui a orientacao profissional. A API continua sendo a autoridade para o que pode ser registrado.
- Quantidade de doses nao e convertida em horarios adicionais arbitrarios. A agenda usa o intervalo informado; para horarios clinicos especificos por dose, o contrato da API ainda precisa fornecer esses horarios.

## Janela e renovacao

- A agenda de medicamentos termina no menor limite entre o fim da prescricao e 72 horas apos a ultima consulta bem-sucedida das prescricoes.
- WorkManager tenta renovar a agenda a cada 12 horas e ao entrar no aplicativo, salvar registros ou ajustar lembretes. Falhas transitorias usam tentativas limitadas com espera exponencial.
- Sem internet ou depois do logout, a agenda ja salva continua funcionando ate seu limite. Sem uma nova consulta autenticada, nao ha extensao indefinida de uma prescricao possivelmente desatualizada.
- Apenas o proximo alarme por medicamento fica registrado no Android. O receptor prepara o seguinte a partir da fila salva, sem chamada de rede.
- Hidratacao e lembretes pontuais renovam apenas o proprio horario apos o disparo, independentemente do login.

## Roteiro em aparelho

1. Instalar a atualizacao sobre a versao anterior. Confirmar que alarmes antigos nao produzem notificacoes duplicadas.
2. Entrar com um paciente e conceder notificacoes. Navegar imediatamente entre Inicio e Registro; o app deve permanecer aberto.
3. Em Perfil > Lembretes, conceder horarios precisos para medicamentos, quando essa opcao aparecer.
4. Registrar REALIZADO em um medicamento de intervalo HORA/1 as 09:00. Conferir o primeiro alerta as 10:00, depois 11:00; repetir com outro item para verificar referencias independentes.
5. Com HORA/10 e registro as 09:00, conferir 19:00, 05:00 e 15:00, sem reiniciar a contagem a cada dia.
6. Repetir um registro, desfazer o inicial e testar uma falha de envio. Nao deve haver duplicacao, nem agenda iniciada por envio malsucedido.
7. Fechar normalmente o app, desligar a internet e depois testar logout. Os alarmes de medicamentos dentro das 72 horas salvas devem continuar, respeitando as permissoes do aparelho.
8. Reiniciar o aparelho e conferir a restauracao dos proximos alarmes. Alterar o fuso e conferir os lembretes diarios.
9. As 12:00, conferir agua e lembrete pontual: um nao deve cancelar o outro.
10. Desativar cada categoria separadamente. Seus alarmes e notificacoes visiveis devem ser cancelados; as outras categorias permanecem.
11. Trocar de paciente. Alarmes e notificacoes do paciente anterior nao devem aparecer para a nova conta.
12. Cancelar uma prescricao no servidor, abrir o app e aguardar a sincronizacao. Conferir a remocao dos alarmes desse item.
13. Revogar notificacoes ou horarios precisos no Android. O app deve permanecer utilizavel ao reabrir, sem SecurityException; sem permissao precisa, usa alarmes aproximados.

## Limites do Android

Alarmes aproximados podem atrasar, sobretudo com economia de bateria. Forcar parada, revogar permissoes ou restricoes do fabricante podem impedir entregas. Revogar acesso a alarmes precisos pode encerrar o processo pelo proprio Android. Nao se deve prometer entrega garantida em todos os aparelhos.

## Verificacao automatizada

`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

Os testes unitarios cobrem referencias independentes, repeticao de registros, desfazer, intervalos, virada de dia, meses curtos, horario de verao, limites numericos e identidades dos lembretes simultaneos. A entrega por AlarmManager, as permissoes e o ciclo real dos receptores exigem validacao em aparelho/emulador.
