'use strict';

module.exports = function (componente) {

  return {

    controller: function (args) {
      this.caso = args.caso;
      this.padrao = !!args.padrao;
      this.titulo = args.titulo;
    },

    view: function (ctrl) {
      var inputNome;
      var className;
      if (ctrl.padrao) {
        inputNome = '';
        className = '';
      } else {
        inputNome = m('input[type=text]', {
          value: ctrl.caso().descricao(),
          onchange: m.withAttr('value', ctrl.caso().descricao)
        });
        className = '.margin-left';
      }

      return m('#' + ctrl.caso().id + '.caso' + className, {
        key: ctrl.caso().id
      }, [

        inputNome,
        m('label.titulo', ctrl.titulo),
        m.component(componente, {
          id: ctrl.caso().id,
          campos: ctrl.caso().campos
        })
      ]);
    }
  };
};