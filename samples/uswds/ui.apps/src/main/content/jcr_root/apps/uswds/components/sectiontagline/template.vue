<template>
  <section class="usa-grid usa-section" v-bind:data-per-path="model.path">
    <div class="usa-width-one-third">
      <h2>A tagline highlights your approach.</h2>
    </div>
    <div class="usa-width-two-thirds">
      <p>The tagline should inspire confidence and interest, focusing on the value
        that your overall approach offers to your audience. Use a heading typeface
        and keep your tagline to just a few words, and don&#x2019;t confuse or
        mystify.</p>
      <p>Use the right side of the grid to explain the tagline a bit more. What
        are your goals? How do you do your work? Write in the present tense, and
        stay brief here. People who are interested can find details on internal
        pages.</p>
    </div>
  </section>
</template>

<script>
    export default {
        props: ['model']
    }
</script>

